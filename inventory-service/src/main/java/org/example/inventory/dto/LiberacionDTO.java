package org.example.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LiberacionDTO {

    @NotNull
    private Long eventId;

    @NotNull
    private Long sectorId;

    private List<Long> asientos;

    @Min(1)
    private Integer cantidad;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resultado {
        private Long eventId;
        private int asientosLiberados;
        private int unidadesLiberadas;
    }
}
