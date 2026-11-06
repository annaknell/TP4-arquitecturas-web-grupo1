package org.example.catalogoservice.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "evento_sectores", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"evento_id", "sector_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoSector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(nullable = false)
    private double precio;

    @Column(name = "capacidad_disponible")
    private Integer capacidadDisponible;
}
