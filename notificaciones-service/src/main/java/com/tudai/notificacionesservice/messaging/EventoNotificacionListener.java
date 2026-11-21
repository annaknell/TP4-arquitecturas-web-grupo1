package com.tudai.notificacionesservice.messaging;

import com.tudai.notificacionesservice.entity.EventoRecibido;
import com.tudai.notificacionesservice.dto.EventoNotificacionMessage;
import com.tudai.notificacionesservice.repository.EventoRecibidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Component
public class EventoNotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(EventoNotificacionListener.class);

    private final EventoRecibidoRepository eventoRecibidoRepository;

    public EventoNotificacionListener(EventoRecibidoRepository eventoRecibidoRepository) {
        this.eventoRecibidoRepository = eventoRecibidoRepository;
    }

    @Transactional
    @RabbitListener(queues = "notificaciones.eventos")
    public void recibir(EventoNotificacionMessage mensaje) {
        validar(mensaje);
        MDC.put("correlationId", mensaje.correlationId());
        try {

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
        } finally {
            MDC.remove("correlationId");
        }
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
