package com.tudai.filavirtualservice.exception;

public class TicketNoEncontradoException extends RuntimeException {
    public TicketNoEncontradoException() {
        super("El cliente no tiene un ticket para este evento");
    }
}
