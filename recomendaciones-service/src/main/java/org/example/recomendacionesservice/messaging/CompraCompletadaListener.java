package org.example.recomendacionesservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.recomendacionesservice.config.RabbitMqConfig;
import org.example.recomendacionesservice.entity.CompraRegistrada;
import org.example.recomendacionesservice.repository.CompraRegistradaRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompraCompletadaListener {

    private final CompraRegistradaRepository compraRegistradaRepository;

    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void onCompraCompletada(Map<String, Object> mensaje) {
        if (mensaje == null) return;
        Object eventType = mensaje.get("eventType");
        if (eventType != null && !"COMPRA_COMPLETADA".equals(eventType.toString())) {
            return;
        }

        Object payloadObj = mensaje.get("payload");
        if (!(payloadObj instanceof Map<?, ?> payload)) {
            return;
        }

        Long compraId = toLong(payload.get("compraId"));
        Long clienteId = toLong(payload.get("clienteId"));
        Long eventoId = toLong(payload.get("eventoId"));

        if (clienteId == null || eventoId == null) {
            log.warn("Evento COMPRA_COMPLETADA sin clienteId o eventoId: {}", payload);
            return;
        }

        if (compraId != null && compraRegistradaRepository.existsByCompraId(compraId)) {
            return;
        }

        compraRegistradaRepository.save(CompraRegistrada.builder()
                .compraId(compraId)
                .clienteId(clienteId)
                .eventoId(eventoId)
                .registradaEn(Instant.now())
                .build());

        log.info("Compra registrada para recomendaciones: clienteId={}, eventoId={}", clienteId, eventoId);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}