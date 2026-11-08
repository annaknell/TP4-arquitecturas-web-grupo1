package org.example.compraservice.dto;

import java.util.List;

public record CompraRequest(
    Long eventoId,
    List<Long> asientos,
    Double precioTotal
) {}