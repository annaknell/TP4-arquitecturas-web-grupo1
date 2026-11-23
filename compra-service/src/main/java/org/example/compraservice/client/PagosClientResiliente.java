package org.example.compraservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.example.compraservice.dto.PagoRequest;
import org.example.compraservice.dto.PagoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cobro con timeouts (configurados en Feign), reintentos y circuit breaker.
 *
 * <p>Los reintentos son seguros: el cobro viaja con {@code Idempotency-Key}, así que
 * pagos no cobra dos veces. El fallback NUNCA devuelve APROBADO: deja el pago en
 * {@code PENDIENTE_RECONCILIACION} para que {@code CompraService} marque la compra
 * como {@code PAGO_INCIERTO} y la tarea de reconciliación resuelva el estado real.</p>
 */
@Component
public class PagosClientResiliente {

    private static final Logger log = LoggerFactory.getLogger(PagosClientResiliente.class);
    public static final String ESTADO_FALLBACK = "PENDIENTE_RECONCILIACION";

    private final PagosClient pagosClient;

    public PagosClientResiliente(PagosClient pagosClient) {
        this.pagosClient = pagosClient;
    }

    @CircuitBreaker(name = "cobroPagos", fallbackMethod = "cobrarFallback")
    @Retry(name = "cobroPagos")
    public PagoResponse cobrar(Long compraId, Long clienteId, Double monto, String idempotencyKey) {
        PagoResponse pago = pagosClient.cobrar(
                idempotencyKey, new PagoRequest(compraId, clienteId, monto, idempotencyKey)).getBody();
        if (pago == null) {
            throw new IllegalStateException("pagos-service no devolvió el resultado del cobro");
        }
        return pago;
    }

    /** Se ejecuta al agotar reintentos o con el circuito abierto. Nunca aprueba un pago. */
    @SuppressWarnings("unused")
    PagoResponse cobrarFallback(Long compraId, Long clienteId, Double monto, String idempotencyKey, Throwable causa) {
        log.warn("Cobro sin respuesta para el cliente {} (idempotencyKey={}): {}. "
                        + "La compra queda PAGO_INCIERTO para reconciliación.",
                clienteId, idempotencyKey, causa.toString());
        return new PagoResponse(null, ESTADO_FALLBACK);
    }
}
