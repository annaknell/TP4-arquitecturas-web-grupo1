package org.example.inventory.repository;

import org.example.inventory.entity.ZonaCupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ZonaCupoRepository extends JpaRepository<ZonaCupo, Long> {

    List<ZonaCupo> findByEventId(Long eventId);

    boolean existsByEventIdAndSectorId(Long eventId, Long sectorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE zona_cupo
               SET retenidos = retenidos + :cantidad, actualizado_en = :ahora
             WHERE event_id = :eventId AND sector_id = :sectorId
               AND capacidad - vendidos - retenidos >= :cantidad
            """, nativeQuery = true)
    int retenerUnidades(@Param("eventId") Long eventId,
                        @Param("sectorId") Long sectorId,
                        @Param("cantidad") int cantidad,
                        @Param("ahora") Instant ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE zona_cupo
               SET retenidos = retenidos - :cantidad, vendidos = vendidos + :cantidad, actualizado_en = :ahora
             WHERE event_id = :eventId AND sector_id = :sectorId AND retenidos >= :cantidad
            """, nativeQuery = true)
    int venderUnidades(@Param("eventId") Long eventId,
                       @Param("sectorId") Long sectorId,
                       @Param("cantidad") int cantidad,
                       @Param("ahora") Instant ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE zona_cupo
               SET retenidos = retenidos - :cantidad, actualizado_en = :ahora
             WHERE event_id = :eventId AND sector_id = :sectorId AND retenidos >= :cantidad
            """, nativeQuery = true)
    int liberarUnidades(@Param("eventId") Long eventId,
                        @Param("sectorId") Long sectorId,
                        @Param("cantidad") int cantidad,
                        @Param("ahora") Instant ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE zona_cupo
               SET vendidos = vendidos - :cantidad, actualizado_en = :ahora
             WHERE event_id = :eventId AND sector_id = :sectorId AND vendidos >= :cantidad
            """, nativeQuery = true)
    int devolverVendidas(@Param("eventId") Long eventId,
                         @Param("sectorId") Long sectorId,
                         @Param("cantidad") int cantidad,
                         @Param("ahora") Instant ahora);
}
