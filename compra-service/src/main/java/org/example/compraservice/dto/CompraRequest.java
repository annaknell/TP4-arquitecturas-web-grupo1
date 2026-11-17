package org.example.compraservice.dto;

import java.util.List;

public record CompraRequest(
    Long eventoId,
    List<Long> asientos,
    Double precioTotal,
    String autorizacionFila
) {
    public CompraRequest(Long eventoId, List<Long> asientos, Double precioTotal) {
        this(eventoId, asientos, precioTotal, null);
    }
}
