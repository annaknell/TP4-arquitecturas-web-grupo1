package org.example.catalogoservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Adaptador HTTP hacia el dueño de la cola; Catálogo no escribe Redis directamente. */
@Component
public class FilaVirtualClient {
    private static final int TAMANIO_TANDA_INICIAL = 10;
    private static final int VENTANA_COMPRA_MINUTOS = 10;

    private final RestClient restClient;

    public FilaVirtualClient(@Value("${fila-virtual-service.url:http://localhost:8086}") String filaVirtualServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(filaVirtualServiceUrl).build();
    }

    public void abrirFila(Long eventoId) {
        restClient.put()
                .uri("/interno/fila/eventos/{eventoId}/configuracion", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ConfiguracionFilaRequest(TAMANIO_TANDA_INICIAL, VENTANA_COMPRA_MINUTOS))
                .retrieve()
                .toBodilessEntity();
    }

    public void cancelarFila(Long eventoId) {
        restClient.put()
                .uri("/interno/fila/eventos/{eventoId}/estado", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EstadoFilaRequest("CANCELADA"))
                .retrieve()
                .toBodilessEntity();
    }

    private record ConfiguracionFilaRequest(int tamanioTanda, int ventanaCompraMinutos) {
    }

    private record EstadoFilaRequest(String estado) {
    }
}
