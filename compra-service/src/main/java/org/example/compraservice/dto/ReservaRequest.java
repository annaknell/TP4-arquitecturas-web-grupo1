package org.example.compraservice.dto;

import java.util.List;

public record ReservaRequest(
    Long eventoId,
    List<Long> asientos
) {}