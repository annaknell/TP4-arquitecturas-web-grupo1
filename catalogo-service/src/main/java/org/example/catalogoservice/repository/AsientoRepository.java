package org.example.catalogoservice.repository;

import org.example.catalogoservice.entity.Asiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AsientoRepository extends JpaRepository<Asiento, Long> {
    List<Asiento> findBySectorId(Long sectorId);
}
