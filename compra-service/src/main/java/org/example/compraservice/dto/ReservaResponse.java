package org.example.compraservice.dto;

import java.math.BigDecimal;

// Foto de la reserva necesaria para emitir y contabilizar las entradas.
public record ReservaResponse(
    Long id,
    String estado,
    Long eventId,
    Long sectorId,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal importeTotal
) {
    /** Compatibilidad con respuestas antiguas mientras se despliega inventario. */
    public ReservaResponse(Long id, String estado) {
        this(id, estado, null, null, null, null, null);
    }
}
