package com.tudai.notificacionesservice.repository;

import com.tudai.notificacionesservice.entity.EventoRecibido;
import com.tudai.notificacionesservice.entity.Notificacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    boolean existsByEventoRecibidoAndDestinatarioAndCanal(
            EventoRecibido eventoRecibido,
            String destinatario,
            Notificacion.Canal canal
    );

    List<Notificacion> findByEstadoInAndProximoIntentoEnLessThanEqualAndIntentosLessThan(
            Collection<Notificacion.Estado> estados,
            LocalDateTime fecha,
            int intentosMaximos
    );

    @EntityGraph(attributePaths = "eventoRecibido")
    @Query("select n from Notificacion n where n.id = :id")
    Optional<Notificacion> findDetalleById(@Param("id") Long id);

    @EntityGraph(attributePaths = "eventoRecibido")
    List<Notificacion> findByEstado(Notificacion.Estado estado);

    @EntityGraph(attributePaths = "eventoRecibido")
    @Query("select n from Notificacion n")
    List<Notificacion> findAllConEvento();
}
