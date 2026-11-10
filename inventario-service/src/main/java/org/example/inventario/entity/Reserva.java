package org.example.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    public enum Estado {
        ACTIVA, CONFIRMADA, LIBERADA, VENCIDA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sector", nullable = false, length = 16)
    private TipoSector tipoSector;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "importe_total", precision = 12, scale = 2)
    private BigDecimal importeTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 16)
    private Estado estado;

    @Column(name = "clave_idempotencia", length = 100)
    private String claveIdempotencia;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "confirmada_en")
    private Instant confirmadaEn;

    @Column(name = "liberada_en")
    private Instant liberadaEn;

    @Column(name = "motivo_liberacion", length = 120)
    private String motivoLiberacion;

    public Reserva(Long eventId, Long sectorId, TipoSector tipoSector, Long clienteId, int cantidad,
                   BigDecimal precioUnitario, Instant expiraEn, String claveIdempotencia) {
        this.eventId = eventId;
        this.sectorId = sectorId;
        this.tipoSector = tipoSector;
        this.clienteId = clienteId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.importeTotal = precioUnitario == null ? null : precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        this.estado = Estado.ACTIVA;
        this.claveIdempotencia = claveIdempotencia;
        this.creadaEn = Instant.now();
        this.expiraEn = expiraEn;
    }

    public boolean estaVencida(Instant ahora) {
        return estado == Estado.ACTIVA && !expiraEn.isAfter(ahora);
    }

    public void confirmar() {
        this.estado = Estado.CONFIRMADA;
        this.confirmadaEn = Instant.now();
    }

    public void liberar(String motivo) {
        this.estado = Estado.LIBERADA;
        this.liberadaEn = Instant.now();
        this.motivoLiberacion = motivo;
    }
}
