package com.tudai.notificacionesservice.messaging;

import com.tudai.notificacionesservice.dto.EventoNotificacionMessage;
import com.tudai.notificacionesservice.entity.EventoRecibido;
import com.tudai.notificacionesservice.repository.EventoRecibidoRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventoNotificacionListenerTest {

    @Test
    void ignoraUnEventoDuplicado() {
        EventoRecibidoRepository repository = mock(EventoRecibidoRepository.class);
        EventoNotificacionListener listener = new EventoNotificacionListener(repository);
        UUID eventId = UUID.randomUUID();
        when(repository.findByEventId(eventId)).thenReturn(Optional.of(new EventoRecibido()));
        EventoNotificacionMessage mensaje = new EventoNotificacionMessage(
                eventId, EventoRecibido.TipoEvento.COMPRA_COMPLETADA, OffsetDateTime.now(),
                "corr-123", "compra-service", Map.of("compraId", 1)
        );

        listener.recibir(mensaje);

        verify(repository, never()).save(any());
    }
}
