package org.example.catalogoservice.messaging;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.example.catalogoservice.config.RabbitMqConfig;
import org.example.catalogoservice.entity.Evento;
import org.example.catalogoservice.entity.EventoSector;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReporteEventoPublisher {
    private final RabbitTemplate rabbitTemplate;

    public ReporteEventoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarConfiguracion(Evento evento, List<EventoSector> sectores, String correlationId) {
        if (evento.getOrganizadorId() == null) {
            throw new IllegalStateException("El evento debe tener organizadorId para generar reportes");
        }
        List<Map<String, Object>> detalleSectores = sectores.stream().map(eventoSector -> {
            int capacidad = eventoSector.getSector().getAsientos().isEmpty()
                    ? eventoSector.getSector().getCapacidad()
                    : eventoSector.getSector().getAsientos().size();
            return Map.<String, Object>of("sectorId", eventoSector.getSector().getId(),
                    "nombre", eventoSector.getSector().getNombre(), "capacidad", capacidad);
        }).toList();
        Map<String, Object> payload = Map.of("eventoId", evento.getId(),
                "organizadorId", evento.getOrganizadorId(), "nombre", evento.getTitulo(),
                "moneda", "ARS", "sectores", detalleSectores);
        Map<String, Object> mensaje = Map.of("eventId", UUID.randomUUID(),
                "eventType", "EVENTO_CONFIGURADO", "occurredAt", OffsetDateTime.now(ZoneOffset.UTC),
                "correlationId", correlationId, "source", "catalogo-service", "payload", payload);
        rabbitTemplate.convertAndSend(RabbitMqConfig.REPORTES_EXCHANGE, "catalogo.evento.configurado", mensaje);
    }
}
