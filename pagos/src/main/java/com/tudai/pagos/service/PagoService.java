package com.tudai.pagos.service;

import com.tudai.pagos.entity.Pago;
import com.tudai.pagos.entity.Reintegro;
import com.tudai.pagos.enums.EstadoPago;
import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.repository.PagoRepository;
import com.tudai.pagos.repository.ReintegroRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PagoService {
    private final PagoRepository pagoRepository;
    private final ReintegroRepository reintegroRepository;

    public PagoService(PagoRepository pagoRepository, ReintegroRepository reintegroRepository) {
        this.pagoRepository = pagoRepository;
        this.reintegroRepository = reintegroRepository;
    }

    public List<Pago> getPagos() {
        return pagoRepository.findAll();
    }
    public Pago getPagoById(Long id) {
        return pagoRepository.findById(id).orElse(null);
    }
    public List<Pago> getPagosByClienteId(Long clienteId) {
        return pagoRepository.findByClienteId(clienteId);
    }
    public void savePago(Pago pago) {
        pagoRepository.save(pago);
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

}
