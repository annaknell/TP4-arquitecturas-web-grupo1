package org.example.repository;

import org.example.entity.Entrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    @Query("SELECT e FROM Entrada e WHERE e.clienteId = :id")
    List<Entrada> findByClienteId(@Param("id") Long id);
}
