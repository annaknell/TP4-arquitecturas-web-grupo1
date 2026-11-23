package org.example.compraservice.dto;

public record PagoRequest(
    Long compraId,
    Long clienteId,
    Double monto,
    String idempotencyKey
) {}