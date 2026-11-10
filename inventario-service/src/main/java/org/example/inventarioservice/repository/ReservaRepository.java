package org.example.inventarioservice.repository;

import org.example.inventarioservice.entity.Reserva;
import org.example.inventarioservice.entity.TipoSector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByClaveIdempotencia(String claveIdempotencia);

    List<Reserva> findByEventIdAndEstado(Long eventId, Reserva.Estado estado);

    List<Reserva> findByEstadoAndExpiraEnBefore(Reserva.Estado estado, Instant ahora);

    @Query("SELECT COALESCE(SUM(r.cantidad), 0) FROM Reserva r WHERE r.eventId = :eventId "
            + "AND r.sectorId = :sectorId AND r.estado = :estado AND r.expiraEn > :ahora")
    long sumarRetenidasEnZona(@Param("eventId") Long eventId,
                              @Param("sectorId") Long sectorId,
                              @Param("estado") Reserva.Estado estado,
                              @Param("ahora") Instant ahora);

    @Query("SELECT COALESCE(SUM(r.cantidad), 0) FROM Reserva r WHERE r.eventId = :eventId "
            + "AND r.clienteId = :clienteId AND r.estado = :estado AND r.expiraEn > :ahora")
    long sumarActivasDeCliente(@Param("eventId") Long eventId,
                               @Param("clienteId") Long clienteId,
                               @Param("estado") Reserva.Estado estado,
                               @Param("ahora") Instant ahora);

    @Query("SELECT COALESCE(SUM(r.cantidad), 0) FROM Reserva r WHERE r.eventId = :eventId "
            + "AND r.clienteId = :clienteId AND r.tipoSector = :tipo AND r.estado = :estado")
    long sumarConfirmadasGeneralesDeCliente(@Param("eventId") Long eventId,
                                            @Param("clienteId") Long clienteId,
                                            @Param("tipo") TipoSector tipo,
                                            @Param("estado") Reserva.Estado estado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.estado = :vencida WHERE r.id = :id AND r.estado = :activa")
    int vencerSiActiva(@Param("id") Long id,
                       @Param("activa") Reserva.Estado activa,
                       @Param("vencida") Reserva.Estado vencida);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.estado = :liberada, r.liberadaEn = :ahora, r.motivoLiberacion = :motivo "
            + "WHERE r.id = :id AND r.estado = :activa")
    int liberarSiActiva(@Param("id") Long id,
                        @Param("activa") Reserva.Estado activa,
                        @Param("liberada") Reserva.Estado liberada,
                        @Param("ahora") Instant ahora,
                        @Param("motivo") String motivo);
}
