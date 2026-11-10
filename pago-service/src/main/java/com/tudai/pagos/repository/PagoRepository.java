package com.tudai.pagos.repository;

import com.tudai.pagos.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    @Query("SELECT p FROM Pago p WHERE p.clienteId = :clienteId")
    List<Pago> findByClienteId(@Param("clienteId") Long clienteId);

    @Query("SELECT p FROM Pago p WHERE p.claveIdempotencia = :claveIdempotencia")
    Optional<Pago> findByClaveIdempotencia(@Param("claveIdempotencia") String claveIdempotencia);

    @Query("SELECT p FROM Pago p WHERE p.compraId = :compraId")
    List<Pago> findByCompraId(@Param("compraId") Long compraId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pago p WHERE p.claveIdempotencia = :claveIdempotencia")
    Optional<Pago> findByClaveIdempotenciaParaActualizar(@Param("claveIdempotencia") String claveIdempotencia);

    @Modifying
    @Transactional
    @Query("UPDATE Pago p SET p.claveIdempotencia = null WHERE p.claveIdempotencia IS NOT NULL AND p.fechaVencimientoIdempotencia <= :ahora")
    int liberarClavesVencidas(@Param("ahora") LocalDateTime ahora);
}
