package org.example.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "evento_config")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionEvento {

    public enum Estado {
        BORRADOR, PUBLICADO, SUSPENDIDO, AGOTADO, CANCELADO, FINALIZADO;

        public boolean aceptaReservas() {
            return this == PUBLICADO;
        }
    }

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 16)
    private Estado estado;

    @Column(name = "max_entradas_por_cliente", nullable = false)
    private int maxEntradasPorCliente;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public ConfiguracionEvento(Long eventId, Estado estado, int maxEntradasPorCliente) {
        this.eventId = eventId;
        this.estado = estado;
        this.maxEntradasPorCliente = maxEntradasPorCliente;
        this.actualizadoEn = Instant.now();
    }
}
