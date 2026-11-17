package com.tudai.filavirtualservice.dto;

import java.time.Instant;

/** Credencial temporal emitida al habilitar la compra. */
public record AutorizacionCompraResponse(Long clienteId, String ticketId, String token, Instant venceEn) {
}
