package com.tudai.pagos.repository;

import com.tudai.pagos.entity.Reintegro;
import com.tudai.pagos.enums.EstadoReintegro;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<Reintegro> findByEstadoReintegro(EstadoReintegro estado);

    List<Reintegro> findByEstadoReintegroInAndFechaActualizacionBefore(
            Collection<EstadoReintegro> estados, LocalDateTime limite);

    @Query("SELECT r.estadoReintegro, COUNT(r) FROM Reintegro r " +
            "WHERE r.compraId = :compraId GROUP BY r.estadoReintegro")
    List<Object[]> contarPorEstadoYCompra(@Param("compraId") Long compraId); 
}
