package org.example.compraservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private Long eventoId;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    private Long reservaId;
    private Long pagoId;

    @Column(nullable = false)
    private Double precioTotal;

    @Column(nullable = false)
    private String estado; // PENDIENTE, COMPLETADA, PAGO_RECHAZADO, RESERVA_VENCIDA, DEVUELTA

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL)
    private List<PasoCompra> pasos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "compra_asientos", joinColumns = @JoinColumn(name = "compra_id"))
    private List<Long> asientos = new ArrayList<>();

    public void addPaso(String descripcion, String estadoPaso) {
        this.pasos.add(new PasoCompra(null, this, descripcion, estadoPaso, LocalDateTime.now()));
    }
}
