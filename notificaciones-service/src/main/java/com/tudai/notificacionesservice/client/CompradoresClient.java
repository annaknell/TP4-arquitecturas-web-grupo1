package com.tudai.notificacionesservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class CompradoresClient {
    private final RestClient restClient;

    public CompradoresClient(@Value("${compra-service.url:http://localhost:8084}") String compraServiceUrl,
                             ClientHttpRequestInterceptor correlationIdInterceptor) {
        this.restClient = RestClient.builder()
                .baseUrl(compraServiceUrl)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    public List<CompradorResponse> buscarPorEvento(Long eventoId) {
        List<CompradorResponse> respuesta = restClient.get()
                .uri("/api/compras/interno/eventos/{eventoId}/compradores", eventoId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<CompradorResponse>>() {});
        return respuesta == null ? List.of() : respuesta;
    }

    public record CompradorResponse(Long clienteId, String email) {}
}
