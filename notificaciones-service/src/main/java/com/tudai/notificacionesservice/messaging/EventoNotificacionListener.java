package com.tudai.notificacionesservice.messaging;

import com.tudai.notificacionesservice.entity.EventoRecibido;
import com.tudai.notificacionesservice.dto.EventoNotificacionMessage;
import com.tudai.notificacionesservice.repository.EventoRecibidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Recibe hechos de negocio y los persiste como inbox antes de que sean procesados
 * para generar y enviar notificaciones.
 */
@Component
public class EventoNotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(EventoNotificacionListener.class);

    private final EventoRecibidoRepository eventoRecibidoRepository;

    public EventoNotificacionListener(EventoRecibidoRepository eventoRecibidoRepository) {
        this.eventoRecibidoRepository = eventoRecibidoRepository;
    }

    /**
     * RabbitMQ confirma el mensaje solo cuando este metodo termina correctamente.
     * Si la transaccion falla, la excepcion se propaga y el broker podra reenviarlo.
     */
    @Transactional
    @RabbitListener(queues = "notificaciones.eventos")
    public void recibir(EventoNotificacionMessage mensaje) {
        validar(mensaje);

        if (eventoRecibidoRepository.findByEventId(mensaje.eventId()).isPresent()) {
            log.info("Evento duplicado ignorado: eventId={}, correlationId={}",
                    mensaje.eventId(), mensaje.correlationId());
            return;
        }

        EventoRecibido evento = new EventoRecibido();
        evento.setEventId(mensaje.eventId());
        evento.setTipo(mensaje.eventType());
        evento.setOrigen(mensaje.source());
        evento.setCorrelacionId(mensaje.correlationId());
        evento.setPayload(mensaje.payload().toString());
        evento.setRecibidoEn(LocalDateTime.now(ZoneOffset.UTC));
        evento.setEstado(EventoRecibido.Estado.RECIBIDO);

        eventoRecibidoRepository.save(evento);
        log.info("Evento recibido y persistido: eventId={}, tipo={}, correlationId={}",
                evento.getEventId(), evento.getTipo(), evento.getCorrelacionId());
    }

    private void validar(EventoNotificacionMessage mensaje) {
        Objects.requireNonNull(mensaje, "El mensaje no puede ser nulo");
        Objects.requireNonNull(mensaje.eventId(), "eventId es obligatorio");
        Objects.requireNonNull(mensaje.eventType(), "eventType es obligatorio");
        Objects.requireNonNull(mensaje.occurredAt(), "occurredAt es obligatorio");
        Objects.requireNonNull(mensaje.correlationId(), "correlationId es obligatorio");
        Objects.requireNonNull(mensaje.source(), "source es obligatorio");
        Objects.requireNonNull(mensaje.payload(), "payload es obligatorio");
    }
}
