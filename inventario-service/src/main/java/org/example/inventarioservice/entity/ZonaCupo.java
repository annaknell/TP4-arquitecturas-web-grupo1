package org.example.inventarioservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "zona_cupo")
@Getter
@Setter
@NoArgsConstructor
public class ZonaCupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "capacidad", nullable = false)
    private int capacidad;

    @Column(name = "vendidos", nullable = false)
    private int vendidos;

    @Column(name = "retenidos", nullable = false)
    private int retenidos;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public ZonaCupo(Long eventId, Long sectorId, int capacidad) {
        this.eventId = eventId;
        this.sectorId = sectorId;
        this.capacidad = capacidad;
        this.vendidos = 0;
        this.retenidos = 0;
        this.actualizadoEn = Instant.now();
    }
}
