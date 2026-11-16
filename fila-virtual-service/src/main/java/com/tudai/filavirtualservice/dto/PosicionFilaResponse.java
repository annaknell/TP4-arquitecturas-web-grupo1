package com.tudai.filavirtualservice.dto;

import com.tudai.filavirtualservice.domain.EstadoTicketFila;
import java.time.Instant;

public record PosicionFilaResponse(
        String ticketId,
        EstadoTicketFila estado,
        Long posicion,
        Long personasDelante,
        Instant habilitadoHasta,
        int intervaloConsultaSugeridoSegundos) {
}
