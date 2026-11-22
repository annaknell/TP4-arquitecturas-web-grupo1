package com.tudai.notificacionesservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Consulta los destinatarios que optaron por avisos de artistas en una ciudad. */
@Component
public class PreferenciasClient {
    private final RestClient restClient;

    public PreferenciasClient(@Value("${recomendaciones-service.url:http://localhost:8087}") String recomendacionesServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(recomendacionesServiceUrl).build();
    }

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

    public record DestinatarioPreferencia(Long clienteId, String email) {}
}
