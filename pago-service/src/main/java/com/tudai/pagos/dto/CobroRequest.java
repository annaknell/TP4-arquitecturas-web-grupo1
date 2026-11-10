package com.tudai.pagos.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CobroRequest(
        @NotNull Long compraId,
        @NotNull Long clienteId,
        @NotNull @Positive Integer monto,
        @NotBlank String moneda) {}
