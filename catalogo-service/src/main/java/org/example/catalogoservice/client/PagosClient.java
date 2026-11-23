package org.example.catalogoservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Notifica a pagos las compras alcanzadas por una cancelación de evento. */
@Component
public class PagosClient {

    private static final Logger log = LoggerFactory.getLogger(PagosClient.class);

    private final RestClient restClient;

    public PagosClient(@Value("${pago-service.url:http://localhost:8085}") String pagoServiceUrl,
                       ClientHttpRequestInterceptor correlationIdInterceptor,
                       RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.clone()
            .baseUrl(pagoServiceUrl)
            .requestInterceptor(correlationIdInterceptor)
            .build();
    }

    @CircuitBreaker(name = "catalogoPagos", fallbackMethod = "registrarFallback")
    public void registrarPendientesPorCancelacion(Long eventoId, List<Long> comprasIds) {
        if (comprasIds.isEmpty()) {
            return;
        }
        restClient.post()
                .uri("/reintegros/cancelacion-evento")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CancelacionEventoRequest(eventoId, comprasIds))
                .retrieve()
                .toBodilessEntity();
    }

    /** El reintegro queda pendiente en pagos; el aviso se registra para poder reintentarlo. */
    @SuppressWarnings("unused")
    void registrarFallback(Long eventoId, List<Long> comprasIds, Throwable causa) {
        log.warn("No se pudieron registrar {} reintegros por cancelación del evento {}: {}",
                comprasIds.size(), eventoId, causa.toString());
    }

    private record CancelacionEventoRequest(Long eventoId, List<Long> comprasIds) {
    }
}
