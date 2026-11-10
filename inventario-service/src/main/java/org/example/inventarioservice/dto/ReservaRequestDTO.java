package org.example.inventarioservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    private Long clienteId;

    @NotNull
    @JsonAlias("eventoId")
    private Long eventId;

    private Long sectorId;

    private List<Long> asientos;

    @Min(1)
    private Integer cantidad;

    private BigDecimal precioUnitario;

    private String claveIdempotencia;
}
