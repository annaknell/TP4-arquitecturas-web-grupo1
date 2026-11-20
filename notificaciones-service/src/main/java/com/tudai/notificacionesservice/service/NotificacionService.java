package com.tudai.notificacionesservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tudai.notificacionesservice.config.ReintentosProperties;
import com.tudai.notificacionesservice.entity.EventoRecibido;
import com.tudai.notificacionesservice.entity.Notificacion;
import com.tudai.notificacionesservice.notification.NotificadorEmail;
import com.tudai.notificacionesservice.repository.EventoRecibidoRepository;
import com.tudai.notificacionesservice.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class NotificacionService {

    private final EventoRecibidoRepository eventoRecibidoRepository;
    private final NotificacionRepository notificacionRepository;
    private final ObjectMapper objectMapper;
    private final NotificadorEmail notificadorEmail;
    private final ReintentosProperties reintentosProperties;

    public NotificacionService(EventoRecibidoRepository eventoRecibidoRepository,
                               NotificacionRepository notificacionRepository,
                               ObjectMapper objectMapper,
                               NotificadorEmail notificadorEmail,
                               ReintentosProperties reintentosProperties) {
        this.eventoRecibidoRepository = eventoRecibidoRepository;
        this.notificacionRepository = notificacionRepository;
        this.objectMapper = objectMapper;
        this.notificadorEmail = notificadorEmail;
        this.reintentosProperties = reintentosProperties;
    }

    /**
     * Genera una notificación pendiente para cada compra completada aún no procesada.
     * La transacción evita que el evento quede como PROCESADO sin haber creado la notificación.
     */
    @Transactional
    public void crearPendientesDesdeEventos() {
        List<EventoRecibido> eventos = eventoRecibidoRepository.findByTipoAndEstado(
                EventoRecibido.TipoEvento.COMPRA_COMPLETADA,
                EventoRecibido.Estado.RECIBIDO
        );

        for (EventoRecibido evento : eventos) {
            crearConfirmacionCompra(evento);
        }
    }

    /** Procesa las notificaciones pendientes cuyo momento de envío ya llegó. */
    @Transactional
    public void procesarPendientes() {
        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        List<Notificacion> pendientes = notificacionRepository
                .findByEstadoInAndProximoIntentoEnLessThanEqualAndIntentosLessThan(
                        List.of(Notificacion.Estado.PENDIENTE, Notificacion.Estado.FALLIDA),
                        ahora,
                        reintentosProperties.getMaximos()
                );

        for (Notificacion notificacion : pendientes) {
            enviar(notificacion);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Notificacion> buscarDetallePorId(Long id) {
        return notificacionRepository.findDetalleById(id);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listar(Notificacion.Estado estado) {
        return estado == null
                ? notificacionRepository.findAllConEvento()
                : notificacionRepository.findByEstado(estado);
    }

    /**
     * Habilita una nueva tanda de reintentos automáticos para una notificación fallida.
     */
    @Transactional
    public Notificacion reintentarManualmente(Long id) {
        Notificacion notificacion = notificacionRepository.findDetalleById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe la notificación " + id));

        if (notificacion.getEstado() != Notificacion.Estado.FALLIDA) {
            throw new IllegalStateException("Sólo se pueden reintentar notificaciones en estado FALLIDA");
        }

        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        notificacion.setEstado(Notificacion.Estado.PENDIENTE);
        notificacion.setIntentos(0);
        notificacion.setUltimoError(null);
        notificacion.setProximoIntentoEn(ahora);
        notificacion.setActualizadaEn(ahora);
        return notificacionRepository.save(notificacion);
    }

    private void enviar(Notificacion notificacion) {
        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        notificacion.setEstado(Notificacion.Estado.EN_PROCESO);
        notificacion.setActualizadaEn(ahora);

        try {
            notificadorEmail.enviar(
                    notificacion.getDestinatario(),
                    notificacion.getAsunto(),
                    notificacion.getContenido()
            );

            notificacion.setEstado(Notificacion.Estado.ENVIADA);
            notificacion.setIntentos(notificacion.getIntentos() + 1);
            notificacion.setEnviadaEn(ahora);
            notificacion.setProximoIntentoEn(null);
            notificacion.setUltimoError(null);
        } catch (Exception ex) {
            int intentosRealizados = notificacion.getIntentos() + 1;
            notificacion.setEstado(Notificacion.Estado.FALLIDA);
            notificacion.setIntentos(intentosRealizados);
            notificacion.setUltimoError(describirError(ex));

            if (intentosRealizados < reintentosProperties.getMaximos()) {
                long demoraSegundos = reintentosProperties
                        .demoraParaSiguienteIntentoSegundos(intentosRealizados);
                notificacion.setProximoIntentoEn(ahora.plusSeconds(demoraSegundos));
            } else {
                // FALLIDA sin próxima fecha significa que se agotaron los reintentos automáticos.
                notificacion.setProximoIntentoEn(null);
            }
        }

        notificacion.setActualizadaEn(ahora);
        notificacionRepository.save(notificacion);
    }

    private String describirError(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }

    private void crearConfirmacionCompra(EventoRecibido evento) {
        JsonNode payload = leerPayload(evento);
        String email = campoObligatorio(payload, "email");
        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);

        if (!notificacionRepository.existsByEventoRecibidoAndDestinatarioAndCanal(
                evento, email, Notificacion.Canal.EMAIL)) {
            Notificacion notificacion = new Notificacion();
            notificacion.setEventoRecibido(evento);
            notificacion.setDestinatario(email);
            notificacion.setCanal(Notificacion.Canal.EMAIL);
            notificacion.setTipo(Notificacion.Tipo.CONFIRMACION_COMPRA);
            notificacion.setAsunto("Confirmación de compra - " + campoObligatorio(payload, "nombreEvento"));
            notificacion.setContenido(construirContenidoConfirmacion(payload));
            notificacion.setEstado(Notificacion.Estado.PENDIENTE);
            notificacion.setIntentos(0);
            notificacion.setProximoIntentoEn(ahora);
            notificacion.setCreadaEn(ahora);
            notificacion.setActualizadaEn(ahora);
            notificacionRepository.save(notificacion);
        }

        evento.setEstado(EventoRecibido.Estado.PROCESADO);
        evento.setProcesadoEn(ahora);
        eventoRecibidoRepository.save(evento);
    }

    private JsonNode leerPayload(EventoRecibido evento) {
        try {
            return objectMapper.readTree(evento.getPayload());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("El payload del evento " + evento.getEventId() + " no es JSON válido", ex);
        }
    }

    private String construirContenidoConfirmacion(JsonNode payload) {
        StringBuilder contenido = new StringBuilder();
        contenido.append("Hola,\n\n")
                .append("Tu compra #").append(campoObligatorio(payload, "compraId"))
                .append(" fue confirmada.\n\n")
                .append("Evento: ").append(campoObligatorio(payload, "nombreEvento")).append('\n')
                .append("Fecha: ").append(campoObligatorio(payload, "fechaEvento")).append('\n')
                .append("Lugar: ").append(campoObligatorio(payload, "lugar")).append("\n\n")
                .append("Entradas:\n");

        JsonNode entradas = payload.path("entradas");
        if (!entradas.isArray() || entradas.isEmpty()) {
            throw new IllegalArgumentException("El evento de compra no contiene entradas");
        }

        for (JsonNode entrada : entradas) {
            contenido.append("- Sector: ").append(campoObligatorio(entrada, "sector"))
                    .append(", asiento: ").append(campoObligatorio(entrada, "asiento"));

            String codigoQr = entrada.path("codigoQr").asText("");
            if (!codigoQr.isBlank()) {
                contenido.append(", código QR: ").append(codigoQr);
            }
            contenido.append('\n');
        }

        return contenido.toString();
    }

    private String campoObligatorio(JsonNode nodo, String nombreCampo) {
        String valor = nodo.path(nombreCampo).asText("");
        if (valor.isBlank()) {
            throw new IllegalArgumentException("El payload no contiene el campo obligatorio '" + nombreCampo + "'");
        }
        return valor;
    }
}
