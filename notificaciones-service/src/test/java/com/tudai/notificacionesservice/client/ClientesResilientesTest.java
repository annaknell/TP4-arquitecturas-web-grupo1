package com.tudai.notificacionesservice.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientesResilientesTest {

    @Test
    @DisplayName("El fallback no devuelve lista vacía: evita marcar el evento como procesado sin avisos")
    void fallbacksNoPierdenNotificaciones() {
        CompradoresClient compradores = new CompradoresClient("http://localhost:8084",
                (request, body, execution) -> execution.execute(request, body), RestClient.builder());
        PreferenciasClient preferencias = new PreferenciasClient("http://localhost:8087", RestClient.builder());
        RuntimeException causa = new RuntimeException("servicio caído");

        assertThrows(IllegalStateException.class,
                () -> compradores.buscarPorEventoFallback(1L, causa));
        assertThrows(IllegalStateException.class,
                () -> preferencias.buscarDestinatariosFallback("Artista", "CABA", causa));
    }
}
