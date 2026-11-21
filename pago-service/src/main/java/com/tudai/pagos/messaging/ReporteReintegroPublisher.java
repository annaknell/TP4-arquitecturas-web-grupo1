package com.tudai.pagos.messaging;

import com.tudai.pagos.config.RabbitMqConfig;
import com.tudai.pagos.entity.Reintegro;
import com.tudai.pagos.enums.EstadoReintegro;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReporteReintegroPublisher {
    private final RabbitTemplate rabbitTemplate;
    public ReporteReintegroPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }

    public void publicar(Reintegro reintegro) {
        String tipo = reintegro.getEstadoReintegro() == EstadoReintegro.APROBADO
                ? "REINTEGRO_APLICADO" : "REINTEGRO_REGISTRADO";
        reintegro.getAsignaciones().forEach(asignacion -> {
            Map<String, Object> payload = Map.of("eventoId", asignacion.getEventoId(),
                    "sectorId", asignacion.getSectorId(), "importeCentavos", asignacion.getImporteCentavos());
            Map<String, Object> evento = Map.of("eventId", UUID.randomUUID(), "eventType", tipo,
                    "occurredAt", OffsetDateTime.now(ZoneOffset.UTC), "correlationId", UUID.randomUUID().toString(),
                    "source", "pago-service", "payload", payload);
            rabbitTemplate.convertAndSend(RabbitMqConfig.REPORTES_EXCHANGE,
                    tipo.equals("REINTEGRO_APLICADO") ? "reintegros.aplicado" : "reintegros.registrado", evento);
        });
    }
}
