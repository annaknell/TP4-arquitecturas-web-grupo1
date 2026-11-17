package com.tudai.filavirtualservice.dto;

import com.tudai.filavirtualservice.domain.EstadoEventoFila;
import jakarta.validation.constraints.NotNull;

public record EstadoEventoFilaRequest(@NotNull EstadoEventoFila estado) {
}
