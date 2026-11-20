package org.example.compraservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** Contrato de la consulta interna a catalogo-service. */
public record EventoNotificacionResponse(
        Long eventoId,
        String nombreEvento,
        LocalDateTime fechaEvento,
        String lugar,
        Map<Long, String> sectoresPorId
) {}
