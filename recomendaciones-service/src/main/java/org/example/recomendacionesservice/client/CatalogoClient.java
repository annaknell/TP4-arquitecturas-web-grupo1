package org.example.recomendacionesservice.client;

import org.example.recomendacionesservice.dto.EventoCatalogoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(@Value("${catalogo.service.url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<EventoCatalogoDto> obtenerEventos() {
        List<EventoCatalogoDto> eventos = restClient.get()
                .uri("/api/eventos")
                .retrieve()
                .body(new ParameterizedTypeReference<List<EventoCatalogoDto>>() {});
        return eventos != null ? eventos : Collections.emptyList();
    }
}