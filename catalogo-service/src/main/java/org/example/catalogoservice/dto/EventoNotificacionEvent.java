package org.example.catalogoservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventoNotificacionEvent(UUID eventId, String eventType, OffsetDateTime occurredAt,
                                      String correlationId, String source, Object payload) {}
