package com.tudai.pagos.controller;

import com.tudai.pagos.config.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class PagosExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PagosExceptionHandler.class);

    /** Formato único de error: status, code, message y traceId. */
    public record ErrorBody(String status, String code, String message, String traceId) {}

    private static ResponseEntity<ErrorBody> responder(int status, String code, String message) {
        String msg = (message == null || message.isBlank()) ? "Error" : message;
        return ResponseEntity.status(status).body(
                new ErrorBody(String.valueOf(status), code, msg, MDC.get(TraceIdFilter.MDC_KEY)));
    }

    @ExceptionHandler({IllegalArgumentException.class,
            MissingRequestHeaderException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorBody> solicitudInvalida(Exception e) {
        return responder(400, "SOLICITUD_INVALIDA",
                e.getMessage() == null ? "Solicitud inválida" : e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorBody> estado(ResponseStatusException e) {
        int status = e.getStatusCode().value();
        String code = switch (status) {
            case 404 -> "NO_ENCONTRADO";
            case 409 -> "CONFLICTO";
            case 422 -> e.getReason() != null && e.getReason().contains("idempotencia")
                    ? "IDEMPOTENCY_KEY_REUTILIZADA" : "REGLA_DE_NEGOCIO";
            default -> "ERROR";
        };
        return responder(status, code, e.getReason());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorBody> estadoInvalido(IllegalStateException e) {
        return responder(409, "CONFLICTO", e.getMessage());
    }

    /** Otro request modificó el mismo registro a la vez (@Version). */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorBody> concurrencia(ObjectOptimisticLockingFailureException e) {
        log.warn("Conflicto de concurrencia: {}", e.getMessage());
        return responder(409, "CONFLICTO_CONCURRENTE",
                "El recurso fue modificado por otra operación simultánea, reintente");
    }

    /** Violación de unicidad u otra restricción de la base. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorBody> integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return responder(409, "CONFLICTO", "La operación viola una restricción de datos");
    }

    /** Cualquier otro error: 500 sin exponer detalles internos, pero con traceId para rastrearlo. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> inesperado(Exception e) {
        // errores de Spring MVC (ruta inexistente, método no soportado, etc.) traen su propio status
        if (e instanceof ErrorResponse er) {
            int status = er.getStatusCode().value();
            return responder(status, status == 404 ? "NO_ENCONTRADO" : "ERROR",
                    er.getBody().getDetail());
        }
        log.error("Error inesperado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ERROR_INTERNO",
                "Error interno del servidor");
    }
}
