package com.tudai.filavirtualservice.dto;

import com.tudai.filavirtualservice.domain.EstadoTicketFila;
import java.time.Instant;

public record IngresoFilaResponse(String ticketId, EstadoTicketFila estado, Instant ingresadoEn, boolean reingreso) {
}
