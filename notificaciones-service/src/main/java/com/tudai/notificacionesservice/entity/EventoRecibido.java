package com.tudai.notificacionesservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "eventos_recibidos")
@Getter
@Setter
@NoArgsConstructor
public class EventoRecibido {

    public enum TipoEvento {
        COMPRA_COMPLETADA, EVENTO_CANCELADO, EVENTO_MODIFICADO
    }
    public enum Estado {
        PENDIENTE, RECIBIDO, PROCESADO, ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column
    @Enumerated(EnumType.STRING)
    private TipoEvento tipo;

    @Column
    private String origen;

    @Column(nullable = false)
    private String correlacionId;

    @Column
    private String payload;

    @Column(nullable = false)
    private LocalDateTime recibidoEn;

    @Column
    private LocalDateTime procesadoEn;

    @Column
    @Enumerated(EnumType.STRING)
    private Estado estado;


}
