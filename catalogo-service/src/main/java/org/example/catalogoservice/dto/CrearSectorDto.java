package org.example.catalogoservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CrearSectorDto(
        @NotNull(message = "El lugarId es obligatorio")
        Long lugarId,
        @NotBlank(message = "El nombre del sector es obligatorio")
        String nombre,
        @Positive(message = "La capacidad debe ser mayor a 0")
        Integer capacidad
) {}