package com.tudai.notificacionesservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.tudai.notificacionesservice.entity.EventoRecibido;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventoNotificacionMessage(
        UUID eventId,
        EventoRecibido.TipoEvento eventType,
        OffsetDateTime occurredAt,
        String correlationId,
        String source,
        JsonNode payload
) {
}
