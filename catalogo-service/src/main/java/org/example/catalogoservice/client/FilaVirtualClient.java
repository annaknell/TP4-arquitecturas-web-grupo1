package org.example.catalogoservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Adaptador HTTP hacia el dueño de la cola; Catálogo no escribe Redis directamente. */
@Component
public class FilaVirtualClient {
    private static final int TAMANIO_TANDA_INICIAL = 10;
    private static final int VENTANA_COMPRA_MINUTOS = 10;

    private final RestClient restClient;

    public FilaVirtualClient(@Value("${fila-virtual-service.url:http://localhost:8086}") String filaVirtualServiceUrl,
                             ClientHttpRequestInterceptor correlationIdInterceptor) {
        this.restClient = RestClient.builder()
                .baseUrl(filaVirtualServiceUrl)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    public void abrirFila(Long eventoId) {
        restClient.put()
                .uri("/api/interno/fila/{eventoId}/configuracion", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ConfiguracionFilaRequest(TAMANIO_TANDA_INICIAL, VENTANA_COMPRA_MINUTOS))
                .retrieve()
                .toBodilessEntity();
    }

    public void cancelarFila(Long eventoId) {
        cambiarEstado(eventoId, "CANCELADA");
    }

    public void pausarFila(Long eventoId) {
        cambiarEstado(eventoId, "PAUSADA");
    }

    public void reabrirFila(Long eventoId) {
        cambiarEstado(eventoId, "ABIERTA");
    }

    private void cambiarEstado(Long eventoId, String estado) {
        restClient.put()
                .uri("/api/interno/fila/{eventoId}/estado", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EstadoFilaRequest(estado))
                .retrieve()
                .toBodilessEntity();
    }

    private record ConfiguracionFilaRequest(int tamanioTanda, int ventanaCompraMinutos) {
    }

    private record EstadoFilaRequest(String estado) {
    }
}
