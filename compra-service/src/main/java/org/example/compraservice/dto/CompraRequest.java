package org.example.compraservice.dto;

import java.util.List;

public record CompraRequest(
    Long eventoId,
    Long sectorId,
    List<Long> asientos,
    Integer cantidad,
    Double precioTotal,
    String autorizacionFila
) {
    /**
     * Una compra es de un único tipo: asientos (sector numerado) o cantidad
     * (zona no numerada). Inventario valida lo mismo al reservar; repetir la
     * regla acá permite rechazar el pedido antes de cobrar.
     */
    public CompraRequest {
        boolean tieneAsientos = asientos != null && !asientos.isEmpty();
        boolean tieneCantidad = cantidad != null && cantidad > 0;

        if (cantidad != null && cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (tieneAsientos == tieneCantidad) {
            throw new IllegalArgumentException("Debe enviar una lista de asientos O un sectorId con cantidad mayor a 0, pero no ambos.");
        }
        if (tieneCantidad && sectorId == null) {
            throw new IllegalArgumentException("Para compras por cantidad debe especificar el sectorId.");
        }
    }
}
