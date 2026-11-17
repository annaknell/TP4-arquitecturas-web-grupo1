package com.tudai.filavirtualservice.controller;

import com.tudai.filavirtualservice.exception.EventoNoHabilitadoException;
import com.tudai.filavirtualservice.exception.TicketNoEncontradoException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(EventoNoHabilitadoException.class)
    ResponseEntity<ErrorResponse> eventoNoHabilitado(EventoNoHabilitadoException exception) {
        return respuesta(HttpStatus.CONFLICT, "EVENTO_NO_HABILITADO", exception.getMessage());
    }

    @ExceptionHandler(TicketNoEncontradoException.class)
    ResponseEntity<ErrorResponse> ticketNoEncontrado(TicketNoEncontradoException exception) {
        return respuesta(HttpStatus.NOT_FOUND, "TICKET_NO_ENCONTRADO", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> solicitudInvalida(IllegalArgumentException exception) {
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", exception.getMessage());
    }

    private ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String codigo, String mensaje) {
        return ResponseEntity.status(status).body(new ErrorResponse(codigo, mensaje, Instant.now()));
    }

    record ErrorResponse(String codigo, String mensaje, Instant timestamp) {
    }
}
