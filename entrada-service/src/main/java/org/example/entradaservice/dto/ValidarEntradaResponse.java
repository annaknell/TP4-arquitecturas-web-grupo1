package org.example.entradaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidarEntradaResponse {
    private boolean autorizado;
    private String motivo;
    private Long sectorId;
    private Long asientoId;
    private Instant usadaEn;
}
