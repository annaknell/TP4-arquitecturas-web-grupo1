package org.example.entradaservice.repository;

import org.example.entradaservice.entity.SolicitudDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudDevolucionRepository extends JpaRepository<SolicitudDevolucion, Long> {
    boolean existsByEntradaIdAndEstadoIn(Long entradaId, List<SolicitudDevolucion.Estado> estados);
    List<SolicitudDevolucion> findByEstado(SolicitudDevolucion.Estado estado);
}
