package com.tudai.filavirtualservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ValidarAutorizacionRequest(@NotNull @Positive Long clienteId, String token) {
}
