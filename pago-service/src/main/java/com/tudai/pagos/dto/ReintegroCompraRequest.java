package com.tudai.pagos.dto;

import com.tudai.pagos.enums.MotivoReintegro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.List;

public record ReintegroCompraRequest(@NotNull MotivoReintegro motivo,
                                     List<@Valid AsignacionReintegroRequest> asignaciones) {}
