package com.tudai.notificacionesservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "notificaciones",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notificacion_evento_destinatario_canal",
                columnNames = {"evento_recibido_id", "destinatario", "canal"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    public enum Canal {
        EMAIL
    }

    public enum Tipo {
        CONFIRMACION_COMPRA,
        CANCELACION_EVENTO,
        MODIFICACION_EVENTO
    }

    public enum Estado {
        PENDIENTE,
        EN_PROCESO,
        ENVIADA,
        FALLIDA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_recibido_id", nullable = false)
    private EventoRecibido eventoRecibido;

    @Column(nullable = false, length = 255)
    private String destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Canal canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Tipo tipo;

    @Column(nullable = false, length = 255)
    private String asunto;

    @Lob
    @Column(nullable = false)
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado;

    @Column(nullable = false)
    private int intentos;

    @Column
    private LocalDateTime proximoIntentoEn;

    @Column
    private LocalDateTime enviadaEn;

    @Lob
    @Column
    private String ultimoError;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    @Column(nullable = false)
    private LocalDateTime actualizadaEn;

    @PrePersist
    private void inicializarProximoIntento() {
        if (proximoIntentoEn == null) {
            proximoIntentoEn = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

}
