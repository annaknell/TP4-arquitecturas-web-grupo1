package org.example.compraservice.messaging;

import org.example.compraservice.config.RabbitMqConfig;
import org.example.compraservice.client.CatalogoClient;
import org.example.compraservice.dto.CompraCompletadaEvent;
import org.example.compraservice.dto.EntradaResponse;
import org.example.compraservice.dto.EventoNotificacionResponse;
import org.example.compraservice.entity.Compra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.List;

/**
 * Publica hechos de negocio hacia Notificaciones. Los errores del broker se
 * registran aquí y nunca se propagan a la transacción de Compra.
 */
@Component
public class CompraCompletadaPublisher {

    private static final Logger log = LoggerFactory.getLogger(CompraCompletadaPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final CatalogoClient catalogoClient;

    public CompraCompletadaPublisher(RabbitTemplate rabbitTemplate, CatalogoClient catalogoClient) {
        this.rabbitTemplate = rabbitTemplate;
        this.catalogoClient = catalogoClient;
    }

    public void publicar(Compra compra, String correlationId, String email, List<EntradaResponse> entradasEmitidas) {
        try {
            EventoNotificacionResponse detalleEvento = obtenerDetalleEvento(compra);
            CompraCompletadaEvent evento = new CompraCompletadaEvent(
                    UUID.randomUUID(), "COMPRA_COMPLETADA", OffsetDateTime.now(ZoneOffset.UTC),
                    correlationId, "compra-service",
                    new CompraCompletadaEvent.Payload(
                            compra.getId(), compra.getClienteId(), email, compra.getEventoId(),
                            detalleEvento.nombreEvento(), detalleEvento.fechaEvento().toString(), detalleEvento.lugar(),
                            entradasEmitidas.stream().map(entrada -> new CompraCompletadaEvent.Entrada(
                                    entrada.id(),
                                    detalleEvento.sectoresPorId().getOrDefault(entrada.sectorId(), "Sector " + entrada.sectorId()),
                                    entrada.asientoId() == null ? "Sin numerar" : "Asiento " + entrada.asientoId(),
                                    entrada.codigoQr()
                            )).toList()
                    )
            );
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE,
                    RabbitMqConfig.ROUTING_KEY_COMPRA_COMPLETADA,
                    evento
            );
            log.info("CompraCompletada publicada: compraId={}, eventId={}, correlationId={}",
                    compra.getId(), evento.eventId(), correlationId);
        } catch (Exception ex) {
            log.error("No se pudo publicar CompraCompletada; la compra permanece completada: compraId={}, correlationId={}",
                    compra.getId(), correlationId, ex);
        }
    }

    private EventoNotificacionResponse obtenerDetalleEvento(Compra compra) {
        try {
            return catalogoClient.obtenerParaNotificacion(compra.getEventoId());
        } catch (Exception ex) {
            log.warn("No se pudo obtener el detalle del evento; se publicará CompraCompletada con datos mínimos: eventoId={}, correlationId={}",
                    compra.getEventoId(), ex);
            return new EventoNotificacionResponse(
                    compra.getEventoId(),
                    "Evento " + compra.getEventoId(),
                    java.time.LocalDateTime.now(ZoneOffset.UTC),
                    "Lugar no disponible",
                    java.util.Map.of()
            );
        }
    }
}
