package com.tudai.filavirtualservice.domain;

import java.time.Instant;

/** Datos persistidos por cliente para asegurar reingreso idempotente. */
public record TicketFila(
        String ticketId,
        Long eventoId,
        Long clienteId,
        EstadoTicketFila estado,
        Instant ingresadoEn,
        Instant habilitadoHasta) {
}
