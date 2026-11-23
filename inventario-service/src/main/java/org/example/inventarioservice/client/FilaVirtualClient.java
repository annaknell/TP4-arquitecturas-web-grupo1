package org.example.inventarioservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Informa a Fila Virtual los cambios de disponibilidad que cierran la venta. */
@Component
public class FilaVirtualClient {
    private final RestClient restClient;

    public FilaVirtualClient(@Value("${fila-virtual-service.url:http://localhost:8086}") String filaVirtualServiceUrl,
                             ClientHttpRequestInterceptor correlationIdInterceptor,
                             RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.clone()
                .baseUrl(filaVirtualServiceUrl)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    public void marcarAgotada(Long eventoId) {
        restClient.put()
                .uri("/api/interno/fila/{eventoId}/estado", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EstadoFilaRequest("AGOTADA"))
                .retrieve()
                .toBodilessEntity();
    }

    public ResultadoAutorizacion validarYConsumir(Long eventoId, Long clienteId, String token) {
        ResultadoAutorizacion resultado = restClient.post()
                .uri("/api/interno/fila/{eventoId}/autorizaciones/validar-y-consumir", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ValidarAutorizacionRequest(clienteId, token))
                .retrieve()
                .body(ResultadoAutorizacion.class);
        return resultado == null ? new ResultadoAutorizacion(false, "TOKEN_INVALIDO") : resultado;
    }

    public ResultadoAutorizacion restaurar(Long eventoId, Long clienteId, String token) {
        ResultadoAutorizacion resultado = restClient.post()
                .uri("/api/interno/fila/{eventoId}/autorizaciones/restaurar", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ValidarAutorizacionRequest(clienteId, token))
                .retrieve()
                .body(ResultadoAutorizacion.class);
        return resultado == null ? new ResultadoAutorizacion(false, "TOKEN_INVALIDO") : resultado;
    }

    private record EstadoFilaRequest(String estado) {
    }

    private record ValidarAutorizacionRequest(Long clienteId, String token) {
    }

    public record ResultadoAutorizacion(boolean autorizada, String motivo) {
    }
}
