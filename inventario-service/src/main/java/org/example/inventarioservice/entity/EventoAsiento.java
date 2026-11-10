package org.example.inventarioservice.entity;

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
@Table(name = "evento_asiento")
@Getter
@Setter
@NoArgsConstructor
public class EventoAsiento {

    public enum Estado {
        LIBRE, RETENIDO, VENDIDO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "asiento_id", nullable = false)
    private Long asientoId;

    @Column(name = "fila", length = 20)
    private String fila;

    @Column(name = "numero", length = 20)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 16)
    private Estado estado;

    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(name = "vence_en")
    private Instant venceEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public EventoAsiento(Long eventId, Long sectorId, Long asientoId, String fila, String numero) {
        this.eventId = eventId;
        this.sectorId = sectorId;
        this.asientoId = asientoId;
        this.fila = fila;
        this.numero = numero;
        this.estado = Estado.LIBRE;
        this.actualizadoEn = Instant.now();
    }

    /** Si el plazo venció, se informa como libre aunque el barrido todavía no lo haya tocado. */
    public Estado estadoEfectivo(Instant ahora) {
        if (estado == Estado.RETENIDO && venceEn != null && !venceEn.isAfter(ahora)) {
            return Estado.LIBRE;
        }
        return estado;
    }
}
