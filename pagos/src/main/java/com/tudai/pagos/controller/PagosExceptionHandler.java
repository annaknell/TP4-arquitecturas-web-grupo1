package com.tudai.pagos.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class PagosExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class,
            MissingRequestHeaderException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> solicitudInvalida(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "400",
                "code", "SOLICITUD_INVALIDA",
                "message", e.getMessage() == null ? "Solicitud inválida" : e.getMessage()));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> estado(ResponseStatusException e) {
        int status = e.getStatusCode().value();
        String code = switch (status) {
            case 404 -> "NO_ENCONTRADO";
            case 409 -> "CONFLICTO";
            case 422 -> e.getReason() != null && e.getReason().contains("idempotencia")
                    ? "IDEMPOTENCY_KEY_REUTILIZADA" : "REGLA_DE_NEGOCIO";
            default -> "ERROR";
        };
        return ResponseEntity.status(status).body(Map.of(
                "status", String.valueOf(status),
                "code", code,
                "message", e.getReason() == null ? "Error" : e.getReason()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> estadoInvalido(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", "409", "code", "CONFLICTO", "message", e.getMessage()));
    }
}
