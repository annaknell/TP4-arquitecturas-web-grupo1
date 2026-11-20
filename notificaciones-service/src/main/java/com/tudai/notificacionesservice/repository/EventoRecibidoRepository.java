package com.tudai.notificacionesservice.repository;


import com.tudai.notificacionesservice.entity.EventoRecibido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventoRecibidoRepository extends JpaRepository<EventoRecibido, Long> {

    Optional<EventoRecibido> findByEventId(UUID eventId);

    List<EventoRecibido> findByTipoAndEstado(
            EventoRecibido.TipoEvento tipo,
            EventoRecibido.Estado estado
    );
}
