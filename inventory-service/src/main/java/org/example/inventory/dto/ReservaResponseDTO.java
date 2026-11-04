package org.example.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.inventory.entity.Reserva;
import org.example.inventory.entity.TipoSector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaResponseDTO {

    private UUID id;
    private Reserva.Estado estado;
    private Long eventId;
    private Long sectorId;
    private TipoSector tipoSector;
    private List<Long> asientos;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal importeTotal;
    private Instant creadaEn;
    private Instant expiraEn;
    private Instant confirmadaEn;

    public static ReservaResponseDTO desde(Reserva reserva, List<Long> asientos) {
        return new ReservaResponseDTO(reserva.getId(), reserva.getEstado(), reserva.getEventId(),
                reserva.getSectorId(), reserva.getTipoSector(), asientos, reserva.getCantidad(),
                reserva.getPrecioUnitario(), reserva.getImporteTotal(), reserva.getCreadaEn(),
                reserva.getExpiraEn(), reserva.getConfirmadaEn());
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resultado {
        private ReservaResponseDTO reserva;
        private boolean nueva;
    }
}
