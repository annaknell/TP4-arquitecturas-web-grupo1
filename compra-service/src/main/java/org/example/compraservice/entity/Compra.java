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
    private String emailCliente;

    @Column(nullable = false)
    private Long eventoId;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    private Long reservaId;
    private Long pagoId;
    private Long sectorId;

    /** Unidades pedidas en zonas no numeradas; null en compras por asiento. */
    private Integer cantidad;

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

    /** Precios y sectores congelados al confirmar la venta; nunca se recalculan desde catálogo. */
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaVenta> lineasVenta = new ArrayList<>();

    public void addPaso(String descripcion, String estadoPaso) {
        this.pasos.add(new PasoCompra(null, this, recortar(descripcion), estadoPaso, LocalDateTime.now()));
    }

    private String recortar(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        return descripcion.length() <= 250 ? descripcion : descripcion.substring(0, 250);
    }
}
