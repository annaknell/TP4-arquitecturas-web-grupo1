package org.example.catalogoservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** Datos de un evento necesarios para confirmar una compra por notificación. */
public record EventoNotificacionResponse(
        Long eventoId,
        String nombreEvento,
        LocalDateTime fechaEvento,
        String lugar,
        Map<Long, String> sectoresPorId
) {}
