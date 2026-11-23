package org.example.inventarioservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Expone el motivo de las reglas de negocio (por ejemplo US-18: tope de entradas
 * por cliente) en el cuerpo de la respuesta 422.
 *
 * <p>Sin este handler, Spring Boot oculta el mensaje por defecto y compra-service
 * recibe un 422 sin detalle, con lo que el cliente no sabría cuántas entradas tiene
 * ni cuál es el límite.</p>
 */
@RestControllerAdvice
public class InventarioExceptionHandler {

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<Map<String, String>> manejarReglaDeNegocio(ReglaDeNegocioException exception) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("message", exception.getMessage()));
    }
}
