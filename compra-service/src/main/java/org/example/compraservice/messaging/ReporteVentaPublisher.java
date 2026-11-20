package org.example.compraservice.messaging;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.example.compraservice.config.RabbitMqConfig;
import org.example.compraservice.entity.LineaVenta;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReporteVentaPublisher {
    private final RabbitTemplate rabbitTemplate;
    public ReporteVentaPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }
    public void publicar(LineaVenta linea, String correlationId) {
        Map<String, Object> evento = Map.of(
                "eventId", UUID.randomUUID(), "eventType", "VENTA_CONFIRMADA",
                "occurredAt", OffsetDateTime.now(ZoneOffset.UTC), "correlationId", correlationId,
                "source", "compra-service", "payload", Map.of("eventoId", linea.getEventoId(),
                        "sectorId", linea.getSectorId(), "importeCentavos", linea.getImporteCentavos()));
        rabbitTemplate.convertAndSend(RabbitMqConfig.REPORTES_EXCHANGE, "ventas.confirmada", evento);
    }
}
