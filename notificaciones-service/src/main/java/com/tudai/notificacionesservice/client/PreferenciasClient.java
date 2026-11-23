package com.tudai.notificacionesservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Consulta los destinatarios que optaron por avisos de artistas en una ciudad. */
@Component
public class PreferenciasClient {
    private static final Logger log = LoggerFactory.getLogger(PreferenciasClient.class);

    private final RestClient restClient;

    public PreferenciasClient(@Value("${recomendaciones-service.url:http://localhost:8087}") String recomendacionesServiceUrl,
                              RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.clone().baseUrl(recomendacionesServiceUrl).build();
    }

    @CircuitBreaker(name = "preferencias", fallbackMethod = "buscarDestinatariosFallback")
    public List<DestinatarioPreferencia> buscarDestinatarios(String artista, String ciudad) {
        List<DestinatarioPreferencia> respuesta = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/preferencias/interno/destinatarios")
                        .queryParam("artista", artista)
                        .queryParam("ciudad", ciudad)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<DestinatarioPreferencia>>() {});
        return respuesta == null ? List.of() : respuesta;
    }

    /** Igual que en compradores: fallamos para no marcar el evento como procesado sin avisos. */
    @SuppressWarnings("unused")
    List<DestinatarioPreferencia> buscarDestinatariosFallback(String artista, String ciudad, Throwable causa) {
        log.warn("No se pudieron consultar preferencias de {} en {}: {}", artista, ciudad, causa.toString());
        throw new IllegalStateException("No se pudieron consultar las preferencias de " + artista, causa);
    }

    public record DestinatarioPreferencia(Long clienteId, String email) {}
}
