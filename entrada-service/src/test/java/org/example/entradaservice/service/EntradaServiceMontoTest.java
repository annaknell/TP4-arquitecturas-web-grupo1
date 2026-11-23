package org.example.entradaservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntradaServiceMontoTest {

    private final EntradaService entradaService = new EntradaService();

    @Test
    @DisplayName("Zona no numerada: el reintegro divide por la cantidad de la compra")
    void montoPorEntradaConCantidad() {
        Map<String, Object> compra = Map.of(
                "precioTotal", 15000.0,
                "asientos", List.of(),
                "cantidad", 3);

        assertEquals(5000, entradaService.montoPorEntrada(compra));
    }

    @Test
    @DisplayName("Sector numerado: el reintegro sigue dividiendo por los asientos")
    void montoPorEntradaConAsientos() {
        Map<String, Object> compra = Map.of(
                "precioTotal", 10000.0,
                "asientos", List.of(101, 102),
                "cantidad", 2);

        assertEquals(5000, entradaService.montoPorEntrada(compra));
    }

    @Test
    @DisplayName("Sin asientos ni cantidad: legacy de una sola entrada")
    void montoPorEntradaLegacy() {
        Map<String, Object> compra = Map.of("precioTotal", 5000.0);

        assertEquals(5000, entradaService.montoPorEntrada(compra));
    }
}
