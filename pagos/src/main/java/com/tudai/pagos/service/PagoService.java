package com.tudai.pagos.service;

import com.tudai.pagos.entity.Pago;
import com.tudai.pagos.enums.EstadoPago;
import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.pasarela.PasarelaNoDisponibleException;
import com.tudai.pagos.pasarela.PasarelaPagos;
import com.tudai.pagos.pasarela.ResultadoCobro;
import com.tudai.pagos.pasarela.SolicitudCobro;
import com.tudai.pagos.repository.PagoRepository;
import com.tudai.pagos.repository.ReintegroRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.tudai.pagos.pasarela.*;
import java.time.LocalDate;
import java.util.*;


@Service
public class PagoService {
    private final PagoRepository pagoRepository;
    private final ReintegroRepository reintegroRepository;
    private final PasarelaPagos pasarela;

    public PagoService(PagoRepository pagoRepository, ReintegroRepository reintegroRepository, PasarelaPagos pasarela) {
        this.pagoRepository = pagoRepository;
        this.reintegroRepository = reintegroRepository;
        this.pasarela = pasarela;
    }

    public List<Pago> getPagos() {
        return pagoRepository.findAll();
    }
    public Pago getPagoById(Long id) {
        return pagoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago " + id + " no encontrado"));
    }
    public List<Pago> getPagosByClienteId(Long clienteId) {
        return pagoRepository.findByClienteId(clienteId);
    }

    private void aprobar(Pago pago, String referencia){
        EstadoPago actual = pago.getEstado();
        if (actual== EstadoPago.APROBADO){
            //si se hace un reintento sobre un pago ya aprobado, no se hace nada.
            return;
        }
        if (actual != EstadoPago.PENDIENTE && actual != EstadoPago.INCIERTO){
            throw new IllegalStateException("No se puede aprobar un pago en estado " + actual);
        }
        pago.setEstado(EstadoPago.APROBADO);
        pago.setReferencia(referencia);
    }
    private void rechazar(Pago pago){
        EstadoPago actual = pago.getEstado();
        if (actual== EstadoPago.RECHAZADO){
            //si se hace un reintento sobre un pago ya rechazado, no se hace nada.
            return;
        }
        if (actual != EstadoPago.PENDIENTE && actual != EstadoPago.INCIERTO){
            throw new IllegalStateException("No se puede rechazar un pago en estado " + actual);
        }
        pago.setEstado(EstadoPago.RECHAZADO);
    }
    private void marcarIncierto(Pago pago){
        EstadoPago actual = pago.getEstado();
        if (actual== EstadoPago.INCIERTO){
            //si se hace un reintento sobre un pago ya incierto, no se hace nada.
            return;
        }
        if (actual != EstadoPago.PENDIENTE){
            throw new IllegalStateException("No se puede marcar como incierto un pago en estado " + actual);
        }
        pago.setEstado(EstadoPago.INCIERTO);
    }
    private boolean coincideCon(Pago pago, String hashRequest) {
        return Objects.equals(pago.getHashRequest(), hashRequest);
    }
    public void aplicarReintegro(Pago pago) {
        EstadoPago actual = pago.getEstado();

        if (actual == EstadoPago.REINTEGRADO) {
            //si el pago fue reintegrado, no se hace nada.
            return;
        }
        if (actual != EstadoPago.APROBADO) {
            throw new IllegalStateException(
                    "No se puede aplicar un reintegro a un pago en estado " + actual);
        }

        long reintegrado = reintegroRepository.sumarMontoPorPagoYEstado(pago.getId(), EstadoReintegro.APROBADO);

        if (reintegrado > pago.getMonto()) {
            throw new IllegalStateException("Lo reintegrado supera el monto cobrado del pago");
        }
        // Si queda saldo, el pago sigue APROBADO
        if (reintegrado == pago.getMonto()) {
            pago.setEstado(EstadoPago.REINTEGRADO);
        }
    }
    public Pago cobrar(String claveIdempotencia, String hashRequest, Long compraId,
                       Long clienteId, Integer monto, String moneda) {

        Optional<Pago> existente = pagoRepository.findByClaveIdempotencia(claveIdempotencia);
        Pago pago;

        if (existente.isPresent()) {
            pago = resultadoGuardado(existente.get(), hashRequest);
            EstadoPago estado = pago.getEstado();
            if (estado != EstadoPago.PENDIENTE && estado != EstadoPago.INCIERTO) {
                return pago; // APROBADO / RECHAZADO / REINTEGRADO: resultado definitivo
            }
            // PENDIENTE o INCIERTO: se reconsulta a la pasarela con la MISMA clave
        } else {
            try {
                pago = pagoRepository.saveAndFlush(new Pago(EstadoPago.PENDIENTE, moneda, monto,
                        clienteId, hashRequest, claveIdempotencia, compraId, LocalDate.now(), null));
            } catch (DataIntegrityViolationException e) {
                // Otro request con la misma clave se nos adelantó: decide la unicidad en la BD
                Pago ganador = pagoRepository.findByClaveIdempotencia(claveIdempotencia)
                        .orElseThrow(() -> e);
                return resultadoGuardado(ganador, hashRequest);
            }
        }
        return procesarEnPasarela(pago);
    }

    private Pago procesarEnPasarela(Pago pago) {
        try {
            ResultadoCobro r = pasarela.cobrar(new SolicitudCobro(
                    pago.getClave_idempotencia(), pago.getMonto(), pago.getMoneda()));
            switch (r.estado()) {
                case APROBADO -> aprobar(pago, r.referencia());
                case RECHAZADO -> rechazar(pago);
                case INCIERTO -> marcarIncierto(pago);
            }
        } catch (PasarelaNoDisponibleException e) {
            marcarIncierto(pago);
        }
        try {
            return pagoRepository.save(pago);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Otro reintento simultáneo ya actualizó el pago: devolvemos lo que quedó guardado
            return pagoRepository.findByClaveIdempotencia(pago.getClave_idempotencia())
                    .orElseThrow(() -> e);
        }
    }

    private Pago resultadoGuardado(Pago pago, String hashRequest) {
        if (!coincideCon(pago, hashRequest)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La clave de idempotencia ya se usó con un pedido distinto");
        }
        return pago;
    }

}
