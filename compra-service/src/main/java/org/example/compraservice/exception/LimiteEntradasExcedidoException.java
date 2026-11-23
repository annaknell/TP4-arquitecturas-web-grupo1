package org.example.compraservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Rechazo por US-18: el cliente supera el tope de entradas configurado para el evento.
 *
 * <p>La regla se valida en inventario (única fuente de verdad). Compra se limita a
 * propagar el 422 y el detalle que devuelve ese servicio, para que el cliente vea
 * cuántas entradas tiene y cuál es el límite.</p>
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class LimiteEntradasExcedidoException extends RuntimeException {

    public LimiteEntradasExcedidoException(String mensaje) {
        super(mensaje);
    }
}
