package org.example.catalogoservice.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientesResilientesTest {

    private static final org.springframework.http.client.ClientHttpRequestInterceptor INTERCEPTOR =
            (request, body, execution) -> execution.execute(request, body);

    @Test
    @DisplayName("Los fallbacks absorben la caída de los servicios aguas abajo")
    void fallbacksNoPropagan() {
        EntradaClient entradas = new EntradaClient("http://localhost:8086", INTERCEPTOR, RestClient.builder());
        PagosClient pagos = new PagosClient("http://localhost:8085", INTERCEPTOR, RestClient.builder());
        FilaVirtualClient fila = new FilaVirtualClient("http://localhost:8086", INTERCEPTOR, RestClient.builder());
        RuntimeException causa = new RuntimeException("servicio caído");

        assertTrue(entradas.cancelarEntradasFallback(1L, causa).isEmpty());
        assertDoesNotThrow(() -> pagos.registrarFallback(1L, List.of(10L), causa));
        assertDoesNotThrow(() -> fila.abrirFilaFallback(1L, causa));
        assertDoesNotThrow(() -> fila.cambiarEstadoFallback(1L, causa));
    }
}
