package com.tudai.pagos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReintegroDevolucionAprobadaRequest(@NotNull Long solicitudDevolucionId,
                                                 @NotNull Long pagoId,
                                                 @NotNull @Positive Integer monto){ }
