package com.tudai.pagos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsignacionReintegroRequest(
        @NotNull Long entradaId,
        @NotNull Long eventoId,
        @NotNull Long sectorId,
        @NotNull @Positive Long importeCentavos) { }
