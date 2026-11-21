package org.example.entradaservice.repository;

import org.example.entradaservice.entity.Entrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    @Query("SELECT e FROM Entrada e WHERE e.clienteId = :id")
    List<Entrada> findByClienteId(@Param("id") Long id);

    List<Entrada> findByEventoIdAndEstadoIn(Long eventoId, List<String> estados);

    Optional<Entrada> findByCodigoQr(String codigoQr);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Entrada e SET e.estado = 'USADA', e.usadaEn = :ahora " +
           "WHERE e.codigoQr = :codigo AND e.estado = 'EMITIDA'")
    int marcarUsada(@Param("codigo") String codigo, @Param("ahora") Instant ahora);
}
