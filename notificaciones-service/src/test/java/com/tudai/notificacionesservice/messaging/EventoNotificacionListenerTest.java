package com.tudai.notificacionesservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tudai.notificacionesservice.dto.EventoNotificacionMessage;
import com.tudai.notificacionesservice.entity.EventoRecibido;
import com.tudai.notificacionesservice.repository.EventoRecibidoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventoNotificacionListenerTest {

    @Test
    void ignoraUnEventoDuplicado() {
        EventoRecibidoRepository repository = mock(EventoRecibidoRepository.class);
        EventoNotificacionListener listener = new EventoNotificacionListener(repository, new ObjectMapper());
        UUID eventId = UUID.randomUUID();
        when(repository.findByEventId(eventId)).thenReturn(Optional.of(new EventoRecibido()));
        EventoNotificacionMessage mensaje = new EventoNotificacionMessage(
                eventId, EventoRecibido.TipoEvento.COMPRA_COMPLETADA, OffsetDateTime.now(),
                "corr-123", "compra-service", Map.of("compraId", 1)
        );

        listener.recibir(mensaje);

        verify(repository, never()).save(any());
    }

    @Test
    void persisteElPayloadComoJsonValido() throws Exception {
        EventoRecibidoRepository repository = mock(EventoRecibidoRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        EventoNotificacionListener listener = new EventoNotificacionListener(repository, objectMapper);
        UUID eventId = UUID.randomUUID();
        EventoNotificacionMessage mensaje = new EventoNotificacionMessage(
                eventId, EventoRecibido.TipoEvento.COMPRA_COMPLETADA, OffsetDateTime.now(),
                "corr-456", "compra-service",
                Map.of("compraId", 42, "evento", Map.of("nombre", "Final"), "items", java.util.List.of(1, 2))
        );

        listener.recibir(mensaje);

        ArgumentCaptor<EventoRecibido> captor = ArgumentCaptor.forClass(EventoRecibido.class);
        verify(repository).save(captor.capture());
        String payload = captor.getValue().getPayload();
        assertTrue(payload.startsWith("{"), "El payload debe ser JSON, no Map.toString(): " + payload);
        com.fasterxml.jackson.databind.JsonNode arbol = objectMapper.readTree(payload);
        assertEquals(42, arbol.get("compraId").asInt());
        assertEquals("Final", arbol.get("evento").get("nombre").asText());
        assertEquals(2, arbol.get("items").size());
    }
}
