package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Entrada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigoQr;

    @Column(nullable = false)
    private String estado; // EMITIDA, USADA, CANCELADA, REINTEGRADA

    // Referencias externas por ID (sin FK relacionales directas a otras bases)
    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "asiento_id")
    private Long asientoId; // Nulo si el sector es no numerado

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "compra_id", nullable = false)
    private Long compraId;
}
