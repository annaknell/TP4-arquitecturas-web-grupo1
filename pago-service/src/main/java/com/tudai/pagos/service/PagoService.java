package com.tudai.pagos.service;

import com.tudai.pagos.entity.Pago;
import com.tudai.pagos.enums.EstadoPago;
import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.repository.PagoRepository;
import com.tudai.pagos.repository.ReintegroRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class PagoService {
    private final PagoRepository pagoRepository;
    private final ReintegroRepository reintegroRepository;
    private final PagoCobroTransaccionalService pagoCobroTransaccionalService;

    public PagoService(PagoRepository pagoRepository,
                       ReintegroRepository reintegroRepository,
                       PagoCobroTransaccionalService pagoCobroTransaccionalService) {
        this.pagoRepository = pagoRepository;
        this.reintegroRepository = reintegroRepository;
        this.pagoCobroTransaccionalService = pagoCobroTransaccionalService;
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

    public List<Pago> getPagosByCompraId(Long compraId) {
        List<Pago> pagos = pagoRepository.findByCompraId(compraId);
        if (pagos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "La compra " + compraId + " no tiene pagos");
        }
        return pagos;
    }

    public Pago getPagoCobradoByCompraId(Long compraId) {
        return getPagosByCompraId(compraId).stream()
                .filter(p -> p.getEstado() == EstadoPago.APROBADO
                        || p.getEstado() == EstadoPago.REINTEGRADO)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "La compra " + compraId + " no tiene un pago cobrado"));
    }

    public void aplicarReintegro(Pago pago) {
        if (pago.getEstado() == EstadoPago.REINTEGRADO) return;
        if (pago.getEstado() != EstadoPago.APROBADO) {
            throw new IllegalStateException("No se puede aplicar un reintegro a un pago en estado " + pago.getEstado());
        }
        long reintegrado = reintegroRepository.sumarMontoPorPagoYEstados(
                pago.getId(), List.of(EstadoReintegro.APROBADO));
        if (reintegrado > pago.getMonto()) {
            throw new IllegalStateException("Lo reintegrado supera el monto cobrado del pago");
        }
        if (reintegrado == pago.getMonto()) {
            pago.setEstado(EstadoPago.REINTEGRADO);
            pagoRepository.save(pago);
        }
    }

    public Pago cobrar(String claveIdempotencia, String hashRequest, Long compraId,
                       Long clienteId, Integer monto, String moneda) {
        try {
            return pagoCobroTransaccionalService.cobrar(
                    claveIdempotencia, hashRequest,
                    compraId, clienteId, monto, moneda);

        } catch (DataIntegrityViolationException ex) {
            // Otra instancia ganó la creación de esa Idempotency-Key.
            return pagoCobroTransaccionalService.obtenerResultadoGanador(
                    claveIdempotencia, hashRequest);
        }
    }

}
