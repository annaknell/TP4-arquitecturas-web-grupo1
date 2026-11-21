package org.example.catalogoservice.repository;

import org.example.catalogoservice.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByEstado(String estado);

    @Query("""
            SELECT DISTINCT e FROM Evento e
            LEFT JOIN e.participantes p
            WHERE e.estado = 'PUBLICADO'
              AND (:texto IS NULL OR LOWER(e.titulo) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:ciudad IS NULL OR LOWER(e.lugar.ciudad) = LOWER(:ciudad))
              AND (:participante IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :participante, '%')))
              AND (:organizadorId IS NULL OR e.organizadorId = :organizadorId)
              AND (:desde IS NULL OR e.fechaInicio >= :desde)
              AND (:hasta IS NULL OR e.fechaInicio <= :hasta)
            """)
    List<Evento> buscar(@Param("texto") String texto,
                        @Param("ciudad") String ciudad,
                        @Param("participante") String participante,
                        @Param("organizadorId") Long organizadorId,
                        @Param("desde") LocalDateTime desde,
                        @Param("hasta") LocalDateTime hasta);
}
