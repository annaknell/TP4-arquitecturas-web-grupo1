package com.tudai.pagos.dto;

import com.tudai.pagos.enums.MotivoReintegro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReintegroRequest(
        @NotNull Long pagoId,
        @NotNull @Positive Integer monto,
        @NotNull MotivoReintegro motivo) {}
