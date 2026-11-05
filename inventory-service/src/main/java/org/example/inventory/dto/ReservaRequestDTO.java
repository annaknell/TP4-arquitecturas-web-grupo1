package org.example.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaRequestDTO {

    @NotNull
    private Long clienteId;

    @NotNull
    private Long eventId;

    @NotNull
    private Long sectorId;

    private List<Long> asientos;

    @Min(1)
    private Integer cantidad;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal precioUnitario;

    private String claveIdempotencia;
}
