package com.tudai.pagos.repository;

import com.tudai.pagos.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {
}
