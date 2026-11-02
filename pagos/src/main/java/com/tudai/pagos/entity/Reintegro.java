package com.tudai.pagos.entity;

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
    private motivoReintegro motivoReintegro;

    @Column
    private EstadoReintegro estadoReintegro;

    @Column
    private Integer intentos;
}
