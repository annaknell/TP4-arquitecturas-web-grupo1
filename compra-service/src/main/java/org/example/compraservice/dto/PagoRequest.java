package org.example.compraservice.dto;

public record PagoRequest(
    Long clienteId,
    Double monto,
    String idempotencyKey
) {}