package org.example.compraservice.repository;
import org.example.compraservice.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {
    Optional<Compra> findByIdempotencyKey(String idempotencyKey);
    List<Compra> findByClienteId(Long clienteId);
    List<Compra> findByEventoId(Long eventoId);
    long countByClienteIdAndEventoIdAndEstado(Long clienteId, Long eventoId, String estado);
}
