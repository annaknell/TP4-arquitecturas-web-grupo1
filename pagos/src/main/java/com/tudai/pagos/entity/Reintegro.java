package com.tudai.pagos.entity;

import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.enums.MotivoReintegro;
import jakarta.persistence.*;

@Entity
public class Reintegro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long pago_id;

    @Column(nullable = false, unique = true)
    private String clave_idempotencia;

    @Column
    private Integer monto;

    @Column
    @Enumerated(EnumType.STRING)
    private MotivoReintegro motivoReintegro;

    @Column
    @Enumerated(EnumType.STRING)
    private EstadoReintegro estadoReintegro;

    @Column
    private Integer intentos;

    public Reintegro() {
    }

    public Reintegro(Long pago_id, String clave_idempotencia, Integer monto, MotivoReintegro motivoReintegro, EstadoReintegro estadoReintegro, Integer intentos) {
        this.pago_id = pago_id;
        this.clave_idempotencia = clave_idempotencia;
        this.monto = monto;
        this.motivoReintegro = motivoReintegro;
        this.estadoReintegro = estadoReintegro;
        this.intentos = intentos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPago_id() {
        return pago_id;
    }

    public void setPago_id(Long pago_id) {
        this.pago_id = pago_id;
    }

    public String getClave_idempotencia() {
        return clave_idempotencia;
    }

    public void setClave_idempotencia(String clave_idempotencia) {
        this.clave_idempotencia = clave_idempotencia;
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
}
