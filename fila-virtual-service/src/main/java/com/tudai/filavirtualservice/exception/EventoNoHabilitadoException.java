package com.tudai.filavirtualservice.exception;

public class EventoNoHabilitadoException extends RuntimeException {
    public EventoNoHabilitadoException() {
        super("El evento no está habilitado para la fila virtual");
    }
}
