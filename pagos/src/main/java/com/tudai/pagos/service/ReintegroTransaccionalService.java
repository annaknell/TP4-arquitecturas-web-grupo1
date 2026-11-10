package com.tudai.pagos.service;

import com.tudai.pagos.dto.ReintegroRequest;
import com.tudai.pagos.entity.Pago;
import com.tudai.pagos.entity.Reintegro;
import com.tudai.pagos.enums.EstadoPago;
import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.enums.MotivoReintegro;
import com.tudai.pagos.pasarela.PasarelaNoDisponibleException;
import com.tudai.pagos.pasarela.PasarelaPagos;
import com.tudai.pagos.pasarela.ResultadoReintegro;
import com.tudai.pagos.pasarela.SolicitudReintegro;
import com.tudai.pagos.repository.ReintegroRepository;
import com.tudai.pagos.util.HashRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ReintegroTransaccionalService {

    private static final int MAX_INTENTOS = 3;
    private static final List<EstadoReintegro> RESERVAN_MONTO =
            List.of(EstadoReintegro.PENDIENTE, EstadoReintegro.INCIERTO, EstadoReintegro.APROBADO);
    private static final Logger audit = LoggerFactory.getLogger("auditoria.pagos");

    private final ReintegroRepository reintegroRepository;
    private final PagoService pagoService;
    private final PasarelaPagos pasarela;

    public ReintegroTransaccionalService(ReintegroRepository reintegroRepository,
                                         PagoService pagoService,
                                         PasarelaPagos pasarela) {
        this.reintegroRepository = reintegroRepository;
        this.pagoService = pagoService;
        this.pasarela = pasarela;
    }

    @Transactional
    public Reintegro reintegrar(Long pagoId, String clave, Integer monto,
                                MotivoReintegro motivo, Long solicitudDevolucionId) {
        String hash = HashRequest.de(new ReintegroRequest(pagoId, monto, motivo));

        Optional<Reintegro> existente = reintegroRepository.findByClaveIdempotenciaParaActualizar(clave);
        if (existente.isPresent()) {
            Reintegro previo = resultadoGuardado(existente.get(), hash);
            audit.info("op=REINTEGRO_REPETIDO reintegroId={} pagoId={} estado={}",
                    previo.getId(), previo.getPagoId(), previo.getEstadoReintegro());
            return previo;
        }

        Pago pago = pagoService.getPagoById(pagoId);
        if (pago.getEstado() != EstadoPago.APROBADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pago no es reintegrable");
        }
        validarMonto(pago, monto);

        // Fuerza el INSERT: si otra instancia ganó la misma clave, falla acá por el UNIQUE.
        Reintegro r = reintegroRepository.saveAndFlush(
                new Reintegro(pagoId, pago.getCompraId(), clave, hash, monto, motivo,
                        solicitudDevolucionId));
        return procesarEnPasarela(r, pago);
    }

    /** Se invoca cuando el INSERT perdió contra otra instancia: devuelve lo que dejó la ganadora. */
    @Transactional
    public Reintegro obtenerResultadoGanador(String clave, Long pagoId, Integer monto, MotivoReintegro motivo) {
        String hash = HashRequest.de(new ReintegroRequest(pagoId, monto, motivo));
        Reintegro ganador = reintegroRepository.findByClaveIdempotenciaParaActualizar(clave)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró el reintegro ganador de la concurrencia"));
        return resultadoGuardado(ganador, hash);
    }

    @Transactional
    public Reintegro procesarEnPasarela(Reintegro r, Pago pago) {
        r.setIntentos(r.getIntentos() + 1);
        try {
            ResultadoReintegro res = pasarela.reintegrar(
                    new SolicitudReintegro(r.getClaveIdempotencia(), pago.getReferencia(), r.getMonto()));
            r.setEstadoReintegro(EstadoReintegro.valueOf(res.estado().name()));
            r.setReferencia(res.referencia());
            r.setDetalle(res.motivo());
        } catch (PasarelaNoDisponibleException ex) {
            audit.warn("op=REINTEGRO_PASARELA_NO_DISPONIBLE reintegroId={} compraId={} detalle={}",
                    r.getId(), r.getCompraId(), ex.getMessage());
            r.setEstadoReintegro(EstadoReintegro.INCIERTO);
            r.setDetalle(ex.getMessage());
        }
        if (r.getIntentos() >= MAX_INTENTOS
                && r.getEstadoReintegro() != EstadoReintegro.APROBADO
                && r.getEstadoReintegro() != EstadoReintegro.RECHAZADO) {
            r.setEstadoReintegro(EstadoReintegro.FALLIDO);
        }
        r.setFechaActualizacion(LocalDateTime.now());
        r = reintegroRepository.save(r);

        audit.info("op=REINTEGRO reintegroId={} pagoId={} compraId={} monto={} motivo={} estado={} intentos={}",
                r.getId(), r.getPagoId(), r.getCompraId(), r.getMonto(),
                r.getMotivoReintegro(), r.getEstadoReintegro(), r.getIntentos());

        if (r.getEstadoReintegro() == EstadoReintegro.APROBADO) {
            pagoService.aplicarReintegro(pago);
        }
        return r;
    }

    private void validarMonto(Pago pago, Integer monto) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        long comprometido = reintegroRepository.sumarMontoPorPagoYEstados(pago.getId(), RESERVAN_MONTO);
        if (comprometido + monto > pago.getMonto()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El reintegro supera el monto pagado");
        }
    }

    private Reintegro resultadoGuardado(Reintegro r, String hash) {
        if (!Objects.equals(r.getHashRequest(), hash)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La clave de idempotencia ya se usó con un pedido distinto");
        }
        return r;
    }
}
