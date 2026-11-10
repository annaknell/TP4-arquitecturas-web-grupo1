package org.example.catalogoservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Notifica a pagos las compras alcanzadas por una cancelación de evento. */
@Component
public class PagosClient {

    private final RestClient restClient;

    public PagosClient(@Value("${pago-service.url:http://localhost:8085}") String pagoServiceUrl) {
        this.restClient = RestClient.builder()
            .baseUrl(pagoServiceUrl)
            .build();
    }

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

    private record CancelacionEventoRequest(Long eventoId, List<Long> comprasIds) {
    }
}
