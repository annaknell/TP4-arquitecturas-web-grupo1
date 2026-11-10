package org.example.catalogoservice.repository;

import org.example.catalogoservice.entity.EventoSector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventoSectorRepository extends JpaRepository<EventoSector, Long> {
    List<EventoSector> findByEventoId(Long eventoId);
    Optional<EventoSector> findByEventoIdAndSectorId(Long eventoId, Long sectorId);
}
