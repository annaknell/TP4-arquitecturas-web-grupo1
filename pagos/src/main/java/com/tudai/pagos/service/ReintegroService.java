package com.tudai.pagos.service;

import com.tudai.pagos.dto.ReintegroRequest;
import com.tudai.pagos.dto.ResumenReintegrosResponse;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.nio.charset.StandardCharsets;

@Service
public class ReintegroService {
    private static final int MAX_INTENTOS = 3;
    private static final List<EstadoReintegro> RESERVAN_MONTO =
            List.of(EstadoReintegro.PENDIENTE, EstadoReintegro.INCIERTO, EstadoReintegro.APROBADO);
    private static final Logger audit = LoggerFactory.getLogger("auditoria.pagos");
    private final ReintegroRepository reintegroRepository;
    private final PagoService pagoService;
    private final PasarelaPagos pasarela;

    public ReintegroService(ReintegroRepository reintegroRepository,
                            PagoService pagoService,
                            PasarelaPagos pasarela) {
        this.reintegroRepository = reintegroRepository;
        this.pagoService = pagoService;
        this.pasarela = pasarela;
    }

    public Reintegro getById(Long id) {
        return reintegroRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Reintegro " + id + " no encontrado"));
    }

    public List<Reintegro> getAll(EstadoReintegro estado) {
        return estado == null
                ? reintegroRepository.findAll()
                : reintegroRepository.findByEstadoReintegro(estado);
    }

    public ResumenReintegrosResponse resumenPorCompra(Long compraId) {
        Map<EstadoReintegro, Long> conteo = new EnumMap<>(EstadoReintegro.class);
        for (Object[] fila : reintegroRepository.contarPorEstadoYCompra(compraId)) {
            conteo.put((EstadoReintegro) fila[0], (Long) fila[1]);
        }
        long total = conteo.values().stream().mapToLong(Long::longValue).sum();
        return new ResumenReintegrosResponse(
                compraId,
                conteo.getOrDefault(EstadoReintegro.PENDIENTE, 0L),
                conteo.getOrDefault(EstadoReintegro.APROBADO, 0L),
                conteo.getOrDefault(EstadoReintegro.RECHAZADO, 0L),
                conteo.getOrDefault(EstadoReintegro.INCIERTO, 0L),
                conteo.getOrDefault(EstadoReintegro.FALLIDO, 0L),
                total);
    }

    public List<Reintegro> getByCompraId(Long compraId) {
        return reintegroRepository.findByCompraId(compraId);
    }
    public List<Reintegro> getByPagoId(Long pagoId) {
        return reintegroRepository.findByPagoId(pagoId);
    }

    public Reintegro reintegrar(Long pagoId, String clave, Integer monto, MotivoReintegro motivo) {
        String hash = HashRequest.de(new ReintegroRequest(pagoId, monto, motivo));

        Optional<Reintegro> existente = reintegroRepository.findByClaveIdempotencia(clave);
        if (existente.isPresent()) {
            Reintegro previo = resultadoGuardado(existente.get(), hash); // idempotente
            audit.info("op=REINTEGRO_REPETIDO reintegroId={} pagoId={} estado={}",
                    previo.getId(), previo.getPagoId(), previo.getEstadoReintegro());
            return previo;
        }

        Pago pago = pagoService.getPagoById(pagoId);
        if (pago.getEstado() != EstadoPago.APROBADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pago no es reintegrable");
        }
        validarMonto(pago, monto);

        Reintegro r;
        try {
            r = reintegroRepository.saveAndFlush(
                    new Reintegro(pagoId, pago.getCompraId(), clave, hash, monto, motivo));
        } catch (DataIntegrityViolationException e) {
            // otro request con la misma clave ganó: decide el unique de la BD
            Reintegro ganador = reintegroRepository.findByClaveIdempotencia(clave)
                    .orElseThrow(() -> e);
            return resultadoGuardado(ganador, hash);
        }
        return procesarEnPasarela(r, pago);
    }

    public Reintegro reintentar(Long id) {
        Reintegro r = getById(id);
        EstadoReintegro e = r.getEstadoReintegro();
        if (e == EstadoReintegro.APROBADO || e == EstadoReintegro.RECHAZADO) {
            return r; // resultado definitivo
        }
        if (e == EstadoReintegro.FALLIDO) { // reintento manual del admin
            r.setIntentos(0);
            r.setEstadoReintegro(EstadoReintegro.PENDIENTE);
        }
        return procesarEnPasarela(r, pagoService.getPagoById(r.getPagoId()));
    }

    private Reintegro procesarEnPasarela(Reintegro r, Pago pago) {
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
            pagoService.aplicarReintegro(pago); // ya con el reintegro persistido
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

    /** Reintegro total de una compra. La clave se deriva, así que repetir la llamada es idempotente. */
    public Reintegro reintegrarPorCompra(Long compraId, MotivoReintegro motivo) {
        Pago pago = pagoService.getPagoCobradoByCompraId(compraId);
        return reintegrar(pago.getId(), claveDerivada(compraId, motivo), pago.getMonto(), motivo);
    }

    static String claveDerivada(Long compraId, MotivoReintegro motivo) {
        String base = "reintegro-compra:" + compraId + ":" + motivo.name();
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
    }


}
