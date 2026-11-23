package org.example.compraservice.dto;

import java.util.List;

/** Cuerpo esperado por POST /api/inventario/liberaciones. */
public record LiberacionRequest(Long eventId, Long sectorId, List<Long> asientos, Integer cantidad) {
}
