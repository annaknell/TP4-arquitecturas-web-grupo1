package org.example.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.inventory.entity.ConfiguracionEvento;
import org.example.inventory.entity.TipoSector;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionDTO {

    @NotNull
    private Long eventId;

    private ConfiguracionEvento.Estado estado;

    @Min(1)
    private Integer maxEntradasPorCliente;

    @NotEmpty
    @Valid
    private List<SectorDef> sectores;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorDef {
        @NotNull
        private Long sectorId;
        @NotNull
        private TipoSector tipo;
        @Positive
        private Integer capacidad;
        private List<AsientoDef> asientos;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AsientoDef {
        @NotNull
        private Long asientoId;
        private String fila;
        private String numero;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resultado {
        private Long eventId;
        private int asientosCreados;
        private int asientosExistentes;
        private int zonasCreadas;
        private int zonasExistentes;
    }
}
