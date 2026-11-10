package org.example.inventarioservice.repository;

import org.example.inventario.entity.EventoAsiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventoAsientoRepository extends JpaRepository<EventoAsiento, Long> {

    List<EventoAsiento> findByEventIdOrderBySectorIdAscAsientoIdAsc(Long eventId);

    List<EventoAsiento> findByEventIdAndSectorIdOrderByAsientoIdAsc(Long eventId, Long sectorId);

    List<EventoAsiento> findByEventIdAndSectorIdAndAsientoIdIn(Long eventId, Long sectorId, List<Long> asientoIds);

    List<EventoAsiento> findByEventIdAndAsientoIdIn(Long eventId, List<Long> asientoIds);

    List<EventoAsiento> findByReservaId(Long reservaId);

    @Query(value = """
            SELECT COUNT(*)
              FROM evento_asiento a
              JOIN reserva r ON r.id = a.reserva_id
             WHERE a.event_id = :eventId
               AND a.estado = 'VENDIDO'
               AND r.cliente_id = :clienteId
            """, nativeQuery = true)
    long contarVendidosDeCliente(@Param("eventId") Long eventId, @Param("clienteId") Long clienteId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE evento_asiento
               SET estado = 'RETENIDO', reserva_id = :reservaId, vence_en = :venceEn, actualizado_en = :ahora
             WHERE id = :asientoRowId AND estado = 'LIBRE'
            """, nativeQuery = true)
    int retener(@Param("asientoRowId") Long asientoRowId,
                @Param("reservaId") Long reservaId,
                @Param("venceEn") Instant venceEn,
                @Param("ahora") Instant ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE evento_asiento
               SET estado = 'VENDIDO', vence_en = NULL, actualizado_en = :ahora
             WHERE id = :asientoRowId AND estado = 'RETENIDO'
               AND reserva_id = :reservaId AND vence_en > :ahora
            """, nativeQuery = true)
    int vender(@Param("asientoRowId") Long asientoRowId,
               @Param("reservaId") Long reservaId,
               @Param("ahora") Instant ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE evento_asiento
               SET estado = 'LIBRE', reserva_id = NULL, vence_en = NULL, actualizado_en = :ahora
             WHERE id = :asientoRowId AND estado = 'RETENIDO' AND reserva_id = :reservaId
            """, nativeQuery = true)
    int liberarRetencion(@Param("asientoRowId") Long asientoRowId,
                         @Param("reservaId") Long reservaId,
                         @Param("ahora") Instant ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE evento_asiento
               SET estado = 'LIBRE', reserva_id = NULL, vence_en = NULL, actualizado_en = :ahora
             WHERE estado = 'RETENIDO' AND vence_en <= :ahora
            """, nativeQuery = true)
    int liberarRetencionesVencidas(@Param("ahora") Instant ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE evento_asiento
               SET estado = 'LIBRE', reserva_id = NULL, actualizado_en = :ahora
             WHERE event_id = :eventId AND sector_id = :sectorId
               AND asiento_id = :asientoId AND estado = 'VENDIDO'
            """, nativeQuery = true)
    int liberarVendido(@Param("eventId") Long eventId,
                       @Param("sectorId") Long sectorId,
                       @Param("asientoId") Long asientoId,
                       @Param("ahora") Instant ahora);
}
