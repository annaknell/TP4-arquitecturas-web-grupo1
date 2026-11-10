package com.tudai.pagos.service;

import com.tudai.pagos.dto.ResumenReintegrosResponse;
import com.tudai.pagos.dto.ResumenReintegrosEventoResponse;
import com.tudai.pagos.entity.Pago;
import com.tudai.pagos.entity.Reintegro;
import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.enums.MotivoReintegro;
import com.tudai.pagos.repository.ReintegroRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.nio.charset.StandardCharsets;
import com.tudai.pagos.dto.ReintegroRequest;
import com.tudai.pagos.util.HashRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReintegroService {
    private static final Logger audit = LoggerFactory.getLogger("auditoria.pagos");
    private static final Logger notificador = LoggerFactory.getLogger("notificador.simulado");
    private static final List<EstadoReintegro> RESERVAN_MONTO = List.of(
            EstadoReintegro.PENDIENTE, EstadoReintegro.INCIERTO, EstadoReintegro.APROBADO);
    private final ReintegroRepository reintegroRepository;
    private final PagoService pagoService;
    private final ReintegroTransaccionalService transaccional;

    public ReintegroService(ReintegroRepository reintegroRepository,
                            PagoService pagoService,
                            ReintegroTransaccionalService transaccional) {
        this.reintegroRepository = reintegroRepository;
        this.pagoService = pagoService;
        this.transaccional = transaccional;
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

    /**
     * Agrupa estados internos para la vista administrativa de una cancelación de evento.
     * Pendientes: PENDIENTE e INCIERTO; completados: APROBADO;
     * fallidos: RECHAZADO y FALLIDO.
     */
    public ResumenReintegrosEventoResponse resumenPorEvento(Long eventoId) {
        Map<EstadoReintegro, Long> conteo = new EnumMap<>(EstadoReintegro.class);
        for (Object[] fila : reintegroRepository.contarPorEstadoYEvento(eventoId)) {
            conteo.put((EstadoReintegro) fila[0], (Long) fila[1]);
        }

        long pendientes = conteo.getOrDefault(EstadoReintegro.PENDIENTE, 0L)
                + conteo.getOrDefault(EstadoReintegro.INCIERTO, 0L);
        long completados = conteo.getOrDefault(EstadoReintegro.APROBADO, 0L);
        long fallidos = conteo.getOrDefault(EstadoReintegro.RECHAZADO, 0L)
                + conteo.getOrDefault(EstadoReintegro.FALLIDO, 0L);

        return new ResumenReintegrosEventoResponse(
                eventoId, pendientes, completados, fallidos,
                pendientes + completados + fallidos);
    }

    public List<Reintegro> getByCompraId(Long compraId) {
        return reintegroRepository.findByCompraId(compraId);
    }

    public Reintegro getBySolicitudDevolucionId(Long solicitudDevolucionId) {
        return reintegroRepository.findBySolicitudDevolucionId(solicitudDevolucionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La solicitud de devolución " + solicitudDevolucionId
                                + " no tiene un reintegro"));
    }

    public List<Reintegro> getByPagoId(Long pagoId) {
        return reintegroRepository.findByPagoId(pagoId);
    }

    public Reintegro reintegrar(Long pagoId, String clave, Integer monto, MotivoReintegro motivo) {
        return reintegrar(pagoId, clave, monto, motivo, null);
    }

    private Reintegro reintegrar(Long pagoId, String clave, Integer monto,
                                 MotivoReintegro motivo, Long solicitudDevolucionId) {
        try {
            return transaccional.reintegrar(pagoId, clave, monto, motivo, solicitudDevolucionId);
        } catch (DataIntegrityViolationException e) {
            // Otra instancia gano la creación de esa clave: se devuelve su resultado.
            return transaccional.obtenerResultadoGanador(clave, pagoId, monto, motivo);
        }
    }

    public Reintegro reintentar(Long id) {
        Reintegro r = getById(id);
        EstadoReintegro e = r.getEstadoReintegro();
        if (e == EstadoReintegro.APROBADO || e == EstadoReintegro.RECHAZADO) {
            return r;
        }
        if (e == EstadoReintegro.FALLIDO) {
            r.setIntentos(0);
            r.setEstadoReintegro(EstadoReintegro.PENDIENTE);
        }
        return transaccional.procesarEnPasarela(r, pagoService.getPagoById(r.getPagoId()));
    }

    /** Reintegro total de una compra (US-29). Clave derivada de compra + motivo: sin cambios. */
    public Reintegro reintegrarPorCompra(Long compraId, MotivoReintegro motivo) {
        Pago pago = pagoService.getPagoCobradoByCompraId(compraId);
        return reintegrar(pago.getId(), claveDerivada(compraId, motivo), pago.getMonto(), motivo);
    }

    /** Reintegro de una devolución individual ya aprobada (US-28). Clave derivada solo de la solicitud. */
    public Reintegro reintegrarDevolucionAprobada(Long solicitudDevolucionId, Long pagoId, Integer monto) {
        return reintegrar(pagoId, claveDerivadaDevolucion(solicitudDevolucionId),
                monto, MotivoReintegro.DEVOLUCION, solicitudDevolucionId);
    }

    static String claveDerivada(Long compraId, MotivoReintegro motivo) {
        String base = "reintegro-compra:" + compraId + ":" + motivo.name();
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** Prefijo propio: nunca colisiona con las claves por compra, y una compra puede tener varias devoluciones. */
    static String claveDerivadaDevolucion(Long solicitudDevolucionId) {
        String base = "reintegro-devolucion:" + solicitudDevolucionId;
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public Optional<Reintegro> registrarPendientePorCancelacion(Long compraId, Long eventoId) {
        MotivoReintegro motivo = MotivoReintegro.CANCELACION_EVENTO;
        String clave = claveDerivadaCancelacion(compraId, eventoId);
        Optional<Reintegro> existente = reintegroRepository.findByClaveIdempotencia(clave);
        if (existente.isPresent()) {
            return existente;
        }
        Pago pago = pagoService.getPagoCobradoByCompraId(compraId);
        long comprometido = reintegroRepository.sumarMontoPorPagoYEstados(pago.getId(), RESERVAN_MONTO);
        long restante = pago.getMonto() - comprometido;
        if (restante <= 0) {
            audit.info("op=REINTEGRO_CANCELACION_OMITIDO compraId={} eventoId={} motivo=sin_monto_restante",
                    compraId, eventoId);
            return Optional.empty();
        }
        int monto = (int) restante;
        String hash = HashRequest.de(new ReintegroRequest(pago.getId(), monto, motivo));
        Reintegro r = new Reintegro(pago.getId(), compraId, clave, hash, monto, motivo, null);
        r.setEventoId(eventoId);
        try {
            r = reintegroRepository.saveAndFlush(r);
        } catch (DataIntegrityViolationException e) {
            return Optional.of(reintegroRepository.findByClaveIdempotencia(clave).orElseThrow(() -> e));
        }

        audit.info("op=REINTEGRO_CANCELACION_REGISTRADO reintegroId={} compraId={} eventoId={} monto={}",
                r.getId(), compraId, eventoId, monto);
        // aviso simulado al comprador (E1);
        notificador.info("Evento cancelado; compra={}; cliente={}; reintegro={}",
                compraId, pago.getClienteId(), r.getId());
        return Optional.of(r);
    }

    /**
     * La cancelación se identifica por compra y evento: una misma compra puede tener
     * entradas de eventos distintos y cada cancelación debe conservar su idempotencia.
     */
    static String claveDerivadaCancelacion(Long compraId, Long eventoId) {
        String base = "reintegro-cancelacion-evento:" + compraId + ":" + eventoId;
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
    }


}
