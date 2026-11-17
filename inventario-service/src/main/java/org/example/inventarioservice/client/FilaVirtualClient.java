package org.example.inventarioservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Informa a Fila Virtual los cambios de disponibilidad que cierran la venta. */
@Component
public class FilaVirtualClient {
    private final RestClient restClient;

    public FilaVirtualClient(@Value("${fila-virtual-service.url:http://localhost:8086}") String filaVirtualServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(filaVirtualServiceUrl).build();
    }

    public void marcarAgotada(Long eventoId) {
        restClient.put()
                .uri("/interno/fila/{eventoId}/estado", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EstadoFilaRequest("AGOTADA"))
                .retrieve()
                .toBodilessEntity();
    }

    private record EstadoFilaRequest(String estado) {
    }
}
