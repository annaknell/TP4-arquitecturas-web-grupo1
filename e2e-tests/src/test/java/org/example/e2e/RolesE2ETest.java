package org.example.e2e;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RolesE2ETest {

    private static final String CUERPO_COMPRA = "{\"eventoId\":1,\"asientos\":[1],\"precioTotal\":1000.0}";
    private static final String CUERPO_EVENTO = "{\"titulo\":\"x\",\"fechaInicio\":\"2026-12-20T21:00:00\","
            + "\"fechaFin\":\"2026-12-20T23:00:00\",\"organizadorId\":1}";

    @Test
    @DisplayName("Sin token los endpoints protegidos responden 401")
    void sinTokenEsNoAutorizado() {
        Assumptions.assumeTrue(Sistema.stackArriba(), "El sistema no esta levantado en " + Sistema.URL);

        assertThat(Sistema.post("/api/compras", CUERPO_COMPRA, null).codigo()).isEqualTo(401);
        assertThat(Sistema.get("/api/entradas/cliente/1", null).codigo()).isEqualTo(401);
        assertThat(Sistema.get("/api/reportes/eventos/1/sectores", null).codigo()).isEqualTo(401);
    }

    @Test
    @DisplayName("Un cliente no puede administrar eventos, validar QR ni ver reportes")
    void clienteNoAccedeARolesAjenos() {
        Assumptions.assumeTrue(Sistema.stackArriba(), "El sistema no esta levantado en " + Sistema.URL);

        String cliente = Sistema.registrarCliente(Sistema.email("rol"));

        assertThat(Sistema.post("/api/eventos?lugarId=1", CUERPO_EVENTO, cliente).codigo()).isEqualTo(403);
        assertThat(Sistema.post("/api/sectores", "{\"lugarId\":1,\"nombre\":\"X\",\"capacidad\":10}", cliente).codigo())
                .as("alta de sectores").isEqualTo(403);
        assertThat(Sistema.post("/api/entradas/validar", "{\"codigoQr\":\"QR-X\",\"eventoId\":1}", cliente).codigo())
                .as("validacion de QR").isEqualTo(403);
        assertThat(Sistema.get("/api/reportes/eventos/1/sectores", cliente).codigo())
                .as("reportes del organizador").isEqualTo(403);
    }

    @Test
    @DisplayName("El administrador accede a los endpoints de gestion")
    void administradorAccede() {
        Assumptions.assumeTrue(Sistema.stackArriba(), "El sistema no esta levantado en " + Sistema.URL);

        String admin = Sistema.tokenAdmin();

        assertThat(Sistema.get("/api/eventos", null).codigo()).isEqualTo(200);
        assertThat(Sistema.get("/api/reportes/eventos/1/sectores", admin).codigo()).isIn(200, 404);
        assertThat(Sistema.get("/api/lugares", admin).codigo()).isEqualTo(200);
    }
}
