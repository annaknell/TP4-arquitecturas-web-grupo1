package com.tudai.pagos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import java.util.List;

public record ReintegroDevolucionAprobadaRequest(@NotNull Long solicitudDevolucionId,
                                                 @NotNull Long pagoId,
                                                 @NotNull @Positive Integer monto,
                                                 List<@Valid AsignacionReintegroRequest> asignaciones){ }
