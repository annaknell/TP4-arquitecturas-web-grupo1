package org.example.catalogoservice;

import org.example.catalogoservice.client.EntradaClient;
import org.example.catalogoservice.client.PagosClient;
import org.example.catalogoservice.entity.Evento;
import org.example.catalogoservice.repository.EventoRepository;
import org.example.catalogoservice.repository.EventoSectorRepository;
import org.example.catalogoservice.repository.LugarRepository;
import org.example.catalogoservice.repository.ParticipanteRepository;
import org.example.catalogoservice.service.EventoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private LugarRepository lugarRepository;

    @Mock
    private ParticipanteRepository participanteRepository;

    @Mock
    private EventoSectorRepository eventoSectorRepository;

    @Mock
    private EntradaClient entradaClient;

    @Mock
    private PagosClient pagosClient;

    @InjectMocks
    private EventoService eventoService;

    @Test
    @DisplayName("Listar solo eventos con estado PUBLICADO")
    void listarEventosPublicados() {
        Evento e1 = new Evento();
        e1.setEstado("PUBLICADO");
        Evento e2 = new Evento();
        e2.setEstado("PUBLICADO");

        when(eventoRepository.findByEstado("PUBLICADO")).thenReturn(List.of(e1, e2));

        List<Evento> resultado = eventoService.listarEventosPublicados();

        assertEquals(2, resultado.size());
        verify(eventoRepository, times(1)).findByEstado("PUBLICADO");
    }

    @Test
    @DisplayName("Cancelar evento notifica a entradaClient y pagosClient")
    void cancelarEventoExitoso() {
        Long eventoId = 10L;
        Evento evento = new Evento();
        evento.setEstado("PUBLICADO");

        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(entradaClient.cancelarEntradasPorEvento(eventoId)).thenReturn(List.of(100L, 101L));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        Evento resultado = eventoService.cancelarEvento(eventoId);

        assertEquals("CANCELADO", resultado.getEstado());
        verify(entradaClient, times(1)).cancelarEntradasPorEvento(eventoId);
        verify(pagosClient, times(1)).registrarPendientesPorCancelacion(eventoId, List.of(100L, 101L));
        verify(eventoRepository, times(1)).save(evento);
    }
}