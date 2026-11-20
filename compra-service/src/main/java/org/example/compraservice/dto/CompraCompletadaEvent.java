package org.example.compraservice.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Sobre de integración publicado una vez que la compra y sus entradas están confirmadas. */
public record CompraCompletadaEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime occurredAt,
        String correlationId,
        String source,
        Payload payload
) {
    public record Payload(
            Long compraId,
            Long clienteId,
            String email,
            Long eventoId,
            String nombreEvento,
            String fechaEvento,
            String lugar,
            List<Entrada> entradas
    ) {}

    public record Entrada(Long entradaId, String sector, String asiento, String codigoQr) {}
}
