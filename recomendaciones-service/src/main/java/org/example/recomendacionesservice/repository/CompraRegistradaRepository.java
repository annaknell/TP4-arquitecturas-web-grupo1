package org.example.recomendacionesservice.repository;

import org.example.recomendacionesservice.entity.CompraRegistrada;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraRegistradaRepository extends MongoRepository<CompraRegistrada, String> {
    List<CompraRegistrada> findByClienteId(Long clienteId);
    List<CompraRegistrada> findByEventoId(Long eventoId);
    List<CompraRegistrada> findByEventoIdIn(List<Long> eventoIds);
    boolean existsByCompraId(Long compraId);
}