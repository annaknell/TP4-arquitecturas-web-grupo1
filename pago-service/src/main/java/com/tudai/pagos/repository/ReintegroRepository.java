package com.tudai.pagos.repository;

import com.tudai.pagos.entity.Reintegro;
import com.tudai.pagos.enums.EstadoReintegro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReintegroRepository extends JpaRepository<Reintegro, Long> {
    @Query("SELECT COALESCE(SUM(r.monto), 0L) FROM Reintegro r " +
            "WHERE r.pagoId = :pagoId AND r.estadoReintegro IN :estados")
    Long sumarMontoPorPagoYEstados(@Param("pagoId") Long pagoId,
                                   @Param("estados") Collection<EstadoReintegro> estados);

    Optional<Reintegro> findByClaveIdempotencia(String claveIdempotencia);

    List<Reintegro> findByPagoId(Long pagoId);

    List<Reintegro> findByCompraId(Long compraId);

    Optional<Reintegro> findBySolicitudDevolucionId(Long solicitudDevolucionId);

    List<Reintegro> findByEstadoReintegro(EstadoReintegro estado);

    List<Reintegro> findByEstadoReintegroInAndFechaActualizacionBefore(Collection<EstadoReintegro> estados, LocalDateTime limite);

    @Query("SELECT r.estadoReintegro, COUNT(r) FROM Reintegro r " +
            "WHERE r.compraId = :compraId GROUP BY r.estadoReintegro")
    List<Object[]> contarPorEstadoYCompra(@Param("compraId") Long compraId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reintegro r WHERE r.claveIdempotencia = :clave")
    Optional<Reintegro> findByClaveIdempotenciaParaActualizar(@Param("clave") String clave);

    List<Reintegro> findByEventoId(Long eventoId);

    @Query("SELECT r.estadoReintegro, COUNT(r) FROM Reintegro r " +
            "WHERE r.eventoId = :eventoId GROUP BY r.estadoReintegro")
    List<Object[]> contarPorEstadoYEvento(@Param("eventoId") Long eventoId);
}
