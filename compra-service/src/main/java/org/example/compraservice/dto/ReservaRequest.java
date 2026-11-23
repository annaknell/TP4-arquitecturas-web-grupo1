package org.example.compraservice.dto;

import java.util.List;

/**
 * Espejo de lo que inventario espera en POST /api/inventario/reservas.
 * `asientos` y `cantidad` son excluyentes; para zonas no numeradas se envía
 * `sectorId` + `cantidad` y `asientos` queda en null.
 */
public record ReservaRequest(
    Long eventoId,
    Long clienteId,
    List<Long> asientos,
    Long sectorId,
    Integer cantidad,
    String autorizacionFila,
    String claveIdempotencia
) {}
