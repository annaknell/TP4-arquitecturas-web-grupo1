package org.example.catalogoservice.messaging;

import org.example.catalogoservice.config.RabbitMqConfig;
import org.example.catalogoservice.dto.EventoNotificacionEvent;
import org.example.catalogoservice.entity.Evento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Component
public class EventoNotificacionPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventoNotificacionPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public EventoNotificacionPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }

    public void publicarCancelacion(Evento evento, String correlationId) {
        publicar("EVENTO_CANCELADO", correlationId, Map.of(
                "eventoId", evento.getId(), "nombreEvento", evento.getTitulo(),
                "motivo", "El evento fue cancelado"
        ));
    }

    public void publicarModificacion(Evento evento, String correlationId, Map<String, Object> cambios) {
        publicar("EVENTO_MODIFICADO", correlationId, Map.of(
                "eventoId", evento.getId(), "nombreEvento", evento.getTitulo(), "cambios", cambios
        ));
    }

    private void publicar(String tipo, String correlationId, Object payload) {
        try {
            EventoNotificacionEvent evento = new EventoNotificacionEvent(UUID.randomUUID(), tipo,
                    OffsetDateTime.now(ZoneOffset.UTC), correlationId, "catalogo-service", payload);
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY_EVENTO, evento);
        } catch (Exception ex) {
            log.error("No se pudo publicar {}. El cambio de Catálogo permanece confirmado: correlationId={}", tipo, correlationId, ex);
        }
    }
}
