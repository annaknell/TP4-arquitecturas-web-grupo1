package org.example.inventarioservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventarioExceptionHandlerTest {

    @Test
    void el422IncluyeElDetalleDeLaReglaDeNegocio() {
        String detalle = "El evento permite 6 entradas por cliente y ya tiene 5";

        ResponseEntity<Map<String, String>> respuesta = new InventarioExceptionHandler()
                .manejarReglaDeNegocio(new ReglaDeNegocioException(detalle));

        assertEquals(422, respuesta.getStatusCode().value());
        assertEquals(detalle, respuesta.getBody().get("message"));
    }
}
