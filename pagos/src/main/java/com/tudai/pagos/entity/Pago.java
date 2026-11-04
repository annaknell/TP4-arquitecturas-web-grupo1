package com.tudai.pagos.entity;

import com.tudai.pagos.enums.EstadoPago;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long compra_id;

    @Column(nullable = false, unique = true)
    private String clave_idempotencia;

    @Column
    private String hashRequest;

    @Column
    private Long cliente_id;

    @Column
    private Integer monto;

    @Column
    private String moneda;

    @Column
    @Enumerated(EnumType.STRING)
    private EstadoPago estadoPago;

    @Column
    private LocalDate fecha_pago;

    @Column
    private String referencia_pasarela;

    @Version
    private Long version;

    public Pago() {}

    public Pago(EstadoPago estadoPago, String moneda, Integer monto, Long cliente_id, String hashRequest, String clave_idempotencia, Long compra_id, LocalDate fecha_pago, String referencia_pasarela) {
        this.estadoPago = estadoPago;
        this.moneda = moneda;
        this.monto = monto;
        this.cliente_id = cliente_id;
        this.hashRequest = hashRequest;
        this.clave_idempotencia = clave_idempotencia;
        this.compra_id = compra_id;
        this.fecha_pago = fecha_pago;
        this.referencia_pasarela = referencia_pasarela;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompra_id() {
        return compra_id;
    }

    public void setCompra_id(Long compra_id) {
        this.compra_id = compra_id;
    }

    public String getClave_idempotencia() {
        return clave_idempotencia;
    }

    public void setClave_idempotencia(String clave_idempotencia) {
        this.clave_idempotencia = clave_idempotencia;
    }

    public String getHashRequest() {
        return hashRequest;
    }

    public void setHashRequest(String hashRequest) {
        this.hashRequest = hashRequest;
    }

    public Long getCliente_id() {
        return cliente_id;
    }

    public void setCliente_id(Long cliente_id) {
        this.cliente_id = cliente_id;
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

    public LocalDate getFecha_pago() {
        return fecha_pago;
    }

    public void setFecha_pago(LocalDate fecha_pago) {
        this.fecha_pago = fecha_pago;
    }

    public String getReferencia() {
        return referencia_pasarela;
    }

    public void setReferencia(String referencia_pasarela) {
        this.referencia_pasarela = referencia_pasarela;
    }
}
