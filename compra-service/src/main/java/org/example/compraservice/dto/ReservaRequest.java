package org.example.compraservice.dto;

import java.util.List;

public record ReservaRequest(
    Long eventoId,
    Long clienteId,
    List<Long> asientos,
    String autorizacionFila,
    String claveIdempotencia
) {}
