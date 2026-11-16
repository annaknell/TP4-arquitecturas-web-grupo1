package com.tudai.filavirtualservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConfiguracionEventoFilaRequest(
        @NotNull @Positive Integer tamanioTanda,
        @NotNull @Positive Integer ventanaCompraMinutos) {
}
