package com.tudai.notificacionesservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class CompradoresClient {
    private static final Logger log = LoggerFactory.getLogger(CompradoresClient.class);

    private final RestClient restClient;

    public CompradoresClient(@Value("${compra-service.url:http://localhost:8084}") String compraServiceUrl,
                             ClientHttpRequestInterceptor correlationIdInterceptor,
                             RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.clone()
                .baseUrl(compraServiceUrl)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @CircuitBreaker(name = "compradores", fallbackMethod = "buscarPorEventoFallback")
    public List<CompradorResponse> buscarPorEvento(Long eventoId) {
        List<CompradorResponse> respuesta = restClient.get()
                .uri("/api/compras/interno/eventos/{eventoId}/compradores", eventoId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<CompradorResponse>>() {});
        return respuesta == null ? List.of() : respuesta;
    }

    /**
     * No devolvemos una lista vacía: eso marcaría el evento como procesado y perdería
     * los avisos. Fallamos rápido para que la transacción se revierta y el scheduler
     * vuelva a intentarlo en el próximo tick.
     */
    @SuppressWarnings("unused")
    List<CompradorResponse> buscarPorEventoFallback(Long eventoId, Throwable causa) {
        log.warn("No se pudieron consultar los compradores del evento {}: {}", eventoId, causa.toString());
        throw new IllegalStateException("No se pudieron consultar los compradores del evento " + eventoId, causa);
    }

    public record CompradorResponse(Long clienteId, String email) {}
}
