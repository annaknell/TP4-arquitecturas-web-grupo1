package com.tudai.pagos.entity;

import com.tudai.pagos.enums.EstadoPago;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long compraId;

    @Column(nullable = false, unique = true)
    private String claveIdempotencia;

    @Column
    private String hashRequest;

    @Column
    private Long clienteId;

    @Column
    private Integer monto;

    @Column
    private String moneda;

    @Column
    @Enumerated(EnumType.STRING)
    private EstadoPago estadoPago;

    @Column
    private LocalDateTime fechaPago;

    @Column
    private String referenciaPasarela;

    @Column
    private String motivoRechazo;

    @Version
    private Long version;

    public Pago() {}

    public Pago(EstadoPago estadoPago, String moneda, Integer monto, Long clienteId, String hashRequest, String claveIdempotencia, Long compraId, LocalDateTime fechaPago, String referenciaPasarela, String motivoRechazo) {
        this.estadoPago = estadoPago;
        this.moneda = moneda;
        this.monto = monto;
        this.clienteId = clienteId;
        this.hashRequest = hashRequest;
        this.claveIdempotencia = claveIdempotencia;
        this.compraId = compraId;
        this.fechaPago = fechaPago;
        this.referenciaPasarela = referenciaPasarela;
        this.motivoRechazo = motivoRechazo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompraId() {
        return compraId;
    }

    public void setCompraId(Long compra_id) {
        this.compraId = compra_id;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(String clave_idempotencia) {
        this.claveIdempotencia = clave_idempotencia;
    }

    public String getHashRequest() {
        return hashRequest;
    }

    public void setHashRequest(String hashRequest) {
        this.hashRequest = hashRequest;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long cliente_id) {
        this.clienteId = cliente_id;
    }

    public Integer getMonto() {
        return monto;
    }

    public void setMonto(Integer monto) {
        this.monto = monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public EstadoPago getEstado() {
        return estadoPago;
    }

    public void setEstado(EstadoPago estadoPago) {
        this.estadoPago = estadoPago;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fecha_pago) {
        this.fechaPago = fecha_pago;
    }

    public String getReferencia() {
        return referenciaPasarela;
    }

    public void setReferencia(String referencia_pasarela) {
        this.referenciaPasarela = referencia_pasarela;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }
}
