package com.tudai.pagos.repository;

import com.tudai.pagos.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    @Query("SELECT p FROM Pago p WHERE p.cliente_id = :clienteId")
    List<Pago> findByClienteId(@Param("clienteId") Long clienteId);
}
