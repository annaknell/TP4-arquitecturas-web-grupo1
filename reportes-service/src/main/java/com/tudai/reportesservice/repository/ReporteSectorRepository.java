package com.tudai.reportesservice.repository;
import java.util.*;
import com.tudai.reportesservice.entity.ReporteSector;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReporteSectorRepository extends JpaRepository<ReporteSector,Long>{
    Optional<ReporteSector> findByEventoEventoIdAndSectorId(Long eventoId, Long sectorId);
    List<ReporteSector> findByEventoEventoId(Long eventoId);
}
