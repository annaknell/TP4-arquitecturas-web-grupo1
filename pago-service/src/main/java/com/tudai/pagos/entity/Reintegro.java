package com.tudai.pagos.entity;

import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.enums.MotivoReintegro;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Reintegro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pagoId;

    @Column(nullable = false, unique = true)
    private String claveIdempotencia;

    @Column(nullable = false)
    private Integer monto;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MotivoReintegro motivoReintegro;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoReintegro estadoReintegro;

    @Column
    private Integer intentos;

    @Column
    private String hashRequest;

    @Column
    private String referenciaPasarela;

    @Column
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column
    private Long compraId;

    @Column(unique = true)
    private Long solicitudDevolucionId;

    @Column
    private Long eventoId;

    @Version
    private Long version;

    public Reintegro() {
    }

    public Reintegro(Long pagoId, Long compraId, String claveIdempotencia, String hashRequest,
                     Integer monto, MotivoReintegro motivo, Long solicitudDevolucionId) {
        this.pagoId = pagoId;
        this.compraId = compraId;
        this.claveIdempotencia = claveIdempotencia;
        this.hashRequest = hashRequest;
        this.monto = monto;
        this.motivoReintegro = motivo;
        this.solicitudDevolucionId = solicitudDevolucionId;
        this.estadoReintegro = EstadoReintegro.PENDIENTE;
        this.intentos = 0;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPagoId() {
        return pagoId;
    }

    public void setPagoId(Long pago_id) {
        this.pagoId = pago_id;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(String clave_idempotencia) {
        this.claveIdempotencia = clave_idempotencia;
    }

    public Integer getMonto() {
        return monto;
    }

    public void setMonto(Integer monto) {
        this.monto = monto;
    }

    public MotivoReintegro getMotivoReintegro() {
        return motivoReintegro;
    }

    public void setMotivoReintegro(MotivoReintegro motivoReintegro) {
        this.motivoReintegro = motivoReintegro;
    }

    public EstadoReintegro getEstadoReintegro() {
        return estadoReintegro;
    }

    public void setEstadoReintegro(EstadoReintegro estadoReintegro) {
        this.estadoReintegro = estadoReintegro;
    }

    public Integer getIntentos() {
        return intentos;
    }

    public void setIntentos(Integer intentos) {
        this.intentos = intentos;
    }

    public String getHashRequest() {
        return hashRequest;
    }

    public void setHashRequest(String hashRequest) {
        this.hashRequest = hashRequest;
    }

    public String getReferencia() {
        return referenciaPasarela;
    }

    public void setReferencia(String referencia_pasarela) {
        this.referenciaPasarela = referencia_pasarela;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public Long getCompraId() {
        return compraId;
    }

    public void setCompraId(Long compraId) {
        this.compraId = compraId;
    }

    public Long getSolicitudDevolucionId() {
        return solicitudDevolucionId;
    }

    public void setSolicitudDevolucionId(Long solicitudDevolucionId) {
        this.solicitudDevolucionId = solicitudDevolucionId;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }
}
