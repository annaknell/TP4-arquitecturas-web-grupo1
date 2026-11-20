package com.tudai.pagos.dto;

import com.tudai.pagos.enums.MotivoReintegro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import java.util.List;

public record ReintegroRequest(
        @NotNull Long pagoId,
        @NotNull @Positive Integer monto,
        @NotNull MotivoReintegro motivo,
        List<@Valid AsignacionReintegroRequest> asignaciones) {
    public ReintegroRequest(Long pagoId, Integer monto, MotivoReintegro motivo) {
        this(pagoId, monto, motivo, List.of());
    }
}
