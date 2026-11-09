package com.tudai.pagos.repository;

import com.tudai.pagos.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    @Query("SELECT p FROM Pago p WHERE p.clienteId = :clienteId")
    List<Pago> findByClienteId(@Param("clienteId") Long clienteId);

    @Query("SELECT p FROM Pago p WHERE p.claveIdempotencia = :claveIdempotencia")
    Optional<Pago> findByClaveIdempotencia(@Param("claveIdempotencia") String claveIdempotencia);

    @Query("SELECT p FROM Pago p WHERE p.compraId = :compraId")
    List<Pago> findByCompraId(@Param("compraId") Long compraId);
}
