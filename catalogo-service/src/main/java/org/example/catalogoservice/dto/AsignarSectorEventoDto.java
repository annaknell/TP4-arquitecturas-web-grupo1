package org.example.catalogoservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsignarSectorEventoDto(
        @NotNull(message = "El sectorId es obligatorio")
        Long sectorId,
        @Positive(message = "El precio debe ser mayor a 0")
        double precio,
        @Positive(message = "La capacidad disponible debe ser mayor a 0")
        Integer capacidadDisponible
) {}