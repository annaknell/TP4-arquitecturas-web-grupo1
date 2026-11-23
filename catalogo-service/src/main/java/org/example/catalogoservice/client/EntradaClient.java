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

/** Cliente del servicio dueño de las entradas. */
@Component
public class EntradaClient {

    private static final Logger log = LoggerFactory.getLogger(EntradaClient.class);

    private final RestClient restClient;

    public EntradaClient(@Value("${entrada-service.url:http://localhost:8086}") String entradaServiceUrl,
                         ClientHttpRequestInterceptor correlationIdInterceptor,
                         RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.clone()
            .baseUrl(entradaServiceUrl)
            .requestInterceptor(correlationIdInterceptor)
            .build();
    }
    @CircuitBreaker(name = "catalogoEntradas", fallbackMethod = "cancelarEntradasFallback")
    public List<Long> cancelarEntradasPorEvento(Long eventoId) {
        List<Long> compraIds = restClient.post()
                .uri("/api/entradas/eventos/{eventoId}/cancelar", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<Long>>() {});
        return compraIds == null ? List.of() : compraIds;
    }

    /** Sin entrada-service no sabemos qué compras cancelar: devolvemos lista vacía y se reintenta luego. */
    @SuppressWarnings("unused")
    List<Long> cancelarEntradasFallback(Long eventoId, Throwable causa) {
        log.warn("No se pudieron cancelar las entradas del evento {}: {}", eventoId, causa.toString());
        return List.of();
    }
}
