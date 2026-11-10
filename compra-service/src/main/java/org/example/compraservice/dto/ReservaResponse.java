package org.example.compraservice.dto;

// lo que devuelve el inventario cuando la reserva es exitosa
public record ReservaResponse(
    Long id,
    String estado
) {}