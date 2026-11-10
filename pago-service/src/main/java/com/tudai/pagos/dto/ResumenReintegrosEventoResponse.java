package com.tudai.pagos.dto;

/** Totales administrativos de reintegros originados por la cancelación de un evento. */
public record ResumenReintegrosEventoResponse(
        Long eventoId,
        long pendientes,
        long completados,
        long fallidos,
        long total) {
}
