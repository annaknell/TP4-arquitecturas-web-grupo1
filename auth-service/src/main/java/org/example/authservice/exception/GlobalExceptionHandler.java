package org.example.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapeo de errores a códigos HTTP: 400 (validación), 401 (credenciales/token),
 * 403 (rol insuficiente) y 409 (email duplicado). Respuestas ProblemDetail
 * (RFC 7807) sin exponer información sensible.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 400: errores de validación de campos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos");
        problem.setTitle("Validación fallida");
        problem.setProperty("errores", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /** 409: email duplicado. */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEmail(DuplicateEmailException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), "Email duplicado");
    }

    /** 401: credenciales inválidas en el login. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", "No autorizado");
    }

    /** 400: reglas de negocio violadas (p.ej. rol CLIENTE en el alta privilegiada). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), "Validación fallida");
    }

    /** 409: violaciones de unicidad a nivel de BD (defensa en profundidad). */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "Conflicto con los datos enviados", "Conflicto");
    }

    /** 403: rol insuficiente (control de acceso por roles). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación", "Prohibido");
    }

    /** 500: error inesperado, sin filtrar detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", "Error interno");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, String title) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}
