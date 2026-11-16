package com.tudai.filavirtualservice.domain;

/** Estado de la fila de un evento de alta demanda. */
public enum EstadoEventoFila {
    ABIERTA,
    PAUSADA,
    AGOTADA,
    CANCELADA
}
