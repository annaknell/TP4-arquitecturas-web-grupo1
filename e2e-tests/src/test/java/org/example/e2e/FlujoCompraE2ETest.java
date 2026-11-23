package org.example.e2e;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlujoCompraE2ETest {

    @Test
    @DisplayName("La compra completa atraviesa catalogo, inventario, compra, pago y entradas")
    void compraCompletaEntreServicios() {
        Assumptions.assumeTrue(Sistema.stackArriba(), "El sistema no esta levantado en " + Sistema.URL);

        String admin = Sistema.tokenAdmin();

        String asientos = "[" + java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(n -> "{\"fila\":\"A\",\"numero\":" + n + "}")
                .reduce((a, b) -> a + "," + b).orElseThrow() + "]";
        Sistema.Respuesta lugar = Sistema.post("/api/lugares",
                "{\"nombre\":\"E2E " + System.nanoTime() + "\",\"direccion\":\"Calle 1\",\"ciudad\":\"Tandil\","
                        + "\"latitud\":-37.32,\"longitud\":-59.13,"
                        + "\"sectores\":[{\"nombre\":\"Platea\",\"capacidad\":10,\"asientos\":" + asientos + "}]}",
                admin);
        assertThat(lugar.codigo()).as("alta del lugar: %s", lugar.cuerpo()).isEqualTo(201);
        long lugarId = lugar.numero("\"id\":(\\d+)");
        long sectorId = lugar.numero("\"sectores\":\\[\\{\"id\":(\\d+)");

        Sistema.Respuesta evento = Sistema.post("/api/eventos?lugarId=" + lugarId,
                "{\"titulo\":\"E2E " + System.nanoTime() + "\",\"fechaInicio\":\"2026-12-20T21:00:00\","
                        + "\"fechaFin\":\"2026-12-20T23:30:00\",\"organizadorId\":1}", admin);
        assertThat(evento.codigo()).as("alta del evento: %s", evento.cuerpo()).isEqualTo(201);
        long eventoId = evento.numero("\"id\":(\\d+)");

        Sistema.Respuesta precio = Sistema.post("/api/eventos/" + eventoId + "/sectores",
                "{\"sectorId\":" + sectorId + ",\"precio\":15000.0,\"capacidadDisponible\":10}", admin);
        assertThat(precio.codigo()).as("precio del sector: %s", precio.cuerpo()).isEqualTo(201);

        assertThat(Sistema.patch("/api/eventos/" + eventoId + "/publicar", admin).codigo()).isEqualTo(200);

        Sistema.Respuesta disponibilidad = Sistema.get("/api/inventario/eventos/" + eventoId + "/disponibilidad", admin);
        assertThat(disponibilidad.codigo()).as("inventario provisionado por catalogo: %s", disponibilidad.cuerpo())
                .isEqualTo(200);
        assertThat(disponibilidad.cuerpo()).contains("\"NUMERADO\"");

        long asientoId = disponibilidad.numero("\"asientoId\":(\\d+)");

        String email = Sistema.email("e2e");
        String cliente = Sistema.registrarCliente(email);
        long clienteId = Long.parseLong(Sistema.post("/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"E2Epass123!\"}", null)
                .texto("\"userId\":(\\d+)"));

        Sistema.Respuesta compra = Sistema.post("/api/compras",
                "{\"eventoId\":" + eventoId + ",\"asientos\":[" + asientoId + "],\"precioTotal\":15000.0}",
                cliente, Sistema.idempotencia("e2e-" + System.nanoTime()));
        assertThat(compra.codigo()).as("compra: %s", compra.cuerpo()).isEqualTo(201);
        assertThat(compra.cuerpo()).contains("\"COMPLETADA\"");
        assertThat(compra.cuerpo()).contains("Inicio de compra")
                .contains("Lugares reservados")
                .contains("Pago aprobado")
                .contains("Reserva confirmada definitivamente")
                .contains("Entradas emitidas");

        Sistema.Respuesta entradas = Sistema.get("/api/entradas/cliente/" + clienteId, cliente);
        assertThat(entradas.codigo()).as("entradas emitidas: %s", entradas.cuerpo()).isEqualTo(200);
        String qr = entradas.texto("\"codigoQr\":\"([^\"]+)\"");
        assertThat(qr).startsWith("QR-");

        assertThat(Sistema.post("/api/entradas/validar",
                "{\"codigoQr\":\"" + qr + "\",\"eventoId\":" + eventoId + "}", admin).codigo())
                .as("primer escaneo").isEqualTo(200);
        assertThat(Sistema.post("/api/entradas/validar",
                "{\"codigoQr\":\"" + qr + "\",\"eventoId\":" + eventoId + "}", admin).codigo())
                .as("segundo escaneo del mismo QR").isEqualTo(409);

        Sistema.Respuesta reporte = Sistema.get("/api/reportes/eventos/" + eventoId + "/sectores", admin);
        assertThat(reporte.codigo()).as("reporte del evento: %s", reporte.cuerpo()).isEqualTo(200);

        Sistema.Respuesta despues = Sistema.get("/api/inventario/eventos/" + eventoId + "/disponibilidad", admin);
        assertThat(despues.cuerpo()).contains("\"estado\":\"VENDIDO\"");
    }
}
