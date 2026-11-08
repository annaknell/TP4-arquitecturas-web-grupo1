package com.tudai.pagos.repository;

import com.tudai.pagos.entity.Reintegro;
import com.tudai.pagos.enums.EstadoReintegro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReintegroRepository extends JpaRepository<Reintegro, Long> {
    @Query("SELECT 0L FROM Reintegro r " +
            "WHERE r.pago_id = :pagoId AND r.estadoReintegro = :estado")
    Long sumarMontoPorPagoYEstado(@Param("pagoId") Long pagoId,
                                  @Param("estado") EstadoReintegro estado);
}
