package org.example.compraservice.dto;

public record EntradaRequest(
    Long eventoId,
    Long asientoId,
    Long clienteId,
    Long compraId
) {}