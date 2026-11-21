package com.tudai.reportesservice.messaging;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ReporteEvent(UUID eventId, String eventType, OffsetDateTime occurredAt,
                           String correlationId, String source, Map<String, Object> payload) { }
