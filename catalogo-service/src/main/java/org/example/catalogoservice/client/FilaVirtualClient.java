package org.example.catalogoservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Adaptador HTTP hacia el dueño de la cola; Catálogo no escribe Redis directamente. */
@Component
public class FilaVirtualClient {
    private static final Logger log = LoggerFactory.getLogger(FilaVirtualClient.class);
    private static final int TAMANIO_TANDA_INICIAL = 10;
    private static final int VENTANA_COMPRA_MINUTOS = 10;

    private final RestClient restClient;

    public FilaVirtualClient(@Value("${fila-virtual-service.url:http://localhost:8086}") String filaVirtualServiceUrl,
                             ClientHttpRequestInterceptor correlationIdInterceptor,
                             RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.clone()
                .baseUrl(filaVirtualServiceUrl)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @CircuitBreaker(name = "catalogoFilaVirtual", fallbackMethod = "abrirFilaFallback")
    public void abrirFila(Long eventoId) {
        restClient.put()
                .uri("/api/interno/fila/{eventoId}/configuracion", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ConfiguracionFilaRequest(TAMANIO_TANDA_INICIAL, VENTANA_COMPRA_MINUTOS))
                .retrieve()
                .toBodilessEntity();
    }

    @CircuitBreaker(name = "catalogoFilaVirtual", fallbackMethod = "cambiarEstadoFallback")
    public void cancelarFila(Long eventoId) {
        cambiarEstado(eventoId, "CANCELADA");
    }

    @CircuitBreaker(name = "catalogoFilaVirtual", fallbackMethod = "cambiarEstadoFallback")
    public void pausarFila(Long eventoId) {
        cambiarEstado(eventoId, "PAUSADA");
    }

    @CircuitBreaker(name = "catalogoFilaVirtual", fallbackMethod = "cambiarEstadoFallback")
    public void reabrirFila(Long eventoId) {
        cambiarEstado(eventoId, "ABIERTA");
    }

    @SuppressWarnings("unused")
    void abrirFilaFallback(Long eventoId, Throwable causa) {
        log.warn("No se pudo abrir la fila del evento {}: {}", eventoId, causa.toString());
    }

    @SuppressWarnings("unused")
    void cambiarEstadoFallback(Long eventoId, Throwable causa) {
        log.warn("No se pudo actualizar el estado de la fila del evento {}: {}", eventoId, causa.toString());
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
