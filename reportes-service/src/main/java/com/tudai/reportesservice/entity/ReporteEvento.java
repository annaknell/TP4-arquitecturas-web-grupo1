package com.tudai.reportesservice.entity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "reportes_evento")
public class ReporteEvento {
    @Id
    private Long eventoId;

    private Long organizadorId;
    private String nombre;
    private String moneda = "ARS";
    private Instant actualizadoEn = Instant.now();

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReporteSector> sectores = new ArrayList<>();

    public Long getEventoId() {
        return eventoId;
    }
    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }
    public Long getOrganizadorId() {
        return organizadorId;
    }
    public void setOrganizadorId(Long organizadorId) {
        this.organizadorId = organizadorId;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getMoneda() {
        return moneda;
    }
    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }
    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
    public void actualizar() {
        actualizadoEn = Instant.now();
    }
    public List<ReporteSector> getSectores() {
        return sectores;
    }
}
