package org.example.inventarioservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.inventario.entity.ConfiguracionEvento;
import org.example.inventario.entity.EventoAsiento;
import org.example.inventario.entity.TipoSector;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadDTO {

    private Long eventId;
    private ConfiguracionEvento.Estado estado;
    private List<SectorDTO> sectores;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorDTO {
        private Long sectorId;
        private TipoSector tipo;
        private Integer capacidad;
        private Integer vendidos;
        private Integer disponibles;
        private List<AsientoDTO> asientos;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AsientoDTO {
        private Long asientoId;
        private String fila;
        private String numero;
        private EventoAsiento.Estado estado;
    }
}
