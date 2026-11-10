package com.tudai.pagos.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CobroRequest(
         Long compraId,
         Long clienteId,
         @Positive Integer monto,
         String moneda) {}
