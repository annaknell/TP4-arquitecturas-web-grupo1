package org.example.compraservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Instantánea inmutable de una entrada vendida, usada por integraciones de reportes. */
@Entity
@Table(name = "lineas_venta")
@Getter
@Setter
@NoArgsConstructor
public class LineaVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(name = "entrada_id", unique = true)
    private Long entradaId;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "asiento_id")
    private Long asientoId;

    @Column(name = "importe_centavos", nullable = false)
    private Long importeCentavos;

    @Column(nullable = false, length = 3)
    private String moneda;

    public LineaVenta(Compra compra, Long entradaId, Long eventoId, Long sectorId,
                      Long asientoId, Long importeCentavos, String moneda) {
        this.compra = compra;
        this.entradaId = entradaId;
        this.eventoId = eventoId;
        this.sectorId = sectorId;
        this.asientoId = asientoId;
        this.importeCentavos = importeCentavos;
        this.moneda = moneda;
    }
}
