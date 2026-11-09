package com.tudai.pagos.dto;

import com.tudai.pagos.enums.MotivoReintegro;
import jakarta.validation.constraints.NotNull;

public record ReintegroCompraRequest(@NotNull MotivoReintegro motivo) {}
