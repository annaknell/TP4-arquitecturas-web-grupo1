package com.tudai.pagos.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** Notificación del servicio de eventos para crear reintegros pendientes. */
public record CancelacionEventoRequest(
        @NotNull @Positive Long eventoId,
        @NotEmpty List<@NotNull @Positive Long> comprasIds) {
}
