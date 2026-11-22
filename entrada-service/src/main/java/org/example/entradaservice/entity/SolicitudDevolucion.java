package org.example.entradaservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "solicitudes_devolucion")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudDevolucion {

    public enum Estado {
        PENDIENTE, APROBADA, RECHAZADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entrada_id", nullable = false)
    private Long entradaId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Estado estado;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "solicitada_en", nullable = false)
    private Instant solicitadaEn;

    @Column(name = "resuelta_en")
    private Instant resueltaEn;
}
