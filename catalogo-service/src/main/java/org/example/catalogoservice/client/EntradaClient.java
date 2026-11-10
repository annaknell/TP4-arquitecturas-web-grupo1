package org.example.catalogoservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Cliente del servicio dueño de las entradas. */
@Component
public class EntradaClient {

    private final RestClient restClient;

    public EntradaClient(RestClient.Builder builder,
                         @Value("${entrada-service.url:http://localhost:8086}") String entradaServiceUrl) {
        this.restClient = builder.baseUrl(entradaServiceUrl).build();
    }

    public List<Long> cancelarEntradasPorEvento(Long eventoId) {
        List<Long> compraIds = restClient.post()
                .uri("/api/entradas/eventos/{eventoId}/cancelar", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<Long>>() {});
        return compraIds == null ? List.of() : compraIds;
    }
}
