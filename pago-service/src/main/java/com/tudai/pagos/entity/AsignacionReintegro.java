package com.tudai.pagos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Distribuye un reintegro entre las entradas/sectores afectados. */
@Entity
@Table(name = "asignaciones_reintegro")
public class AsignacionReintegro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reintegro_id", nullable = false)
    private Reintegro reintegro;

    @Column(name = "entrada_id", nullable = false)
    private Long entradaId;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "importe_centavos", nullable = false)
    private Long importeCentavos;

    public AsignacionReintegro(Reintegro reintegro, Long entradaId, Long eventoId, Long sectorId, Long importeCentavos) {
        this.reintegro = reintegro;
        this.entradaId = entradaId;
        this.eventoId = eventoId;
        this.sectorId = sectorId;
        this.importeCentavos = importeCentavos;
    }

    public AsignacionReintegro() { }

    public Long getEntradaId() { return entradaId; }
    public Long getEventoId() { return eventoId; }
    public Long getSectorId() { return sectorId; }
    public Long getImporteCentavos() { return importeCentavos; }
}
