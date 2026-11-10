package org.example.compraservice.dto;

public record EntradaResponse(
    Long id,
    String codigoQr,
    String estado,
    Long eventoId,
    Long sectorId,
    Long asientoId,
    Long clienteId,
    Long compraId
) {}