package org.example.catalogoservice.service;

import org.example.catalogoservice.entity.Evento;
import org.example.catalogoservice.entity.Lugar;
import org.example.catalogoservice.repository.EventoRepository;
import org.example.catalogoservice.repository.LugarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EventoServiceTest {

    @Mock
    private EventoRepository eRepository;

    @Mock
    private LugarRepository lugarRepository;

    @InjectMocks
    private EventoService eService;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void crearEventoDeberiaAsignarLugarYGuardarExitosamente(){
        Long lugarId = 1L;
        Lugar lugar = new Lugar();
        lugar.setId(lugarId);

        Evento evento = new Evento();
        evento.setTitulo("Recital de Rock");

        when(lugarRepository.findById(lugarId)).thenReturn(Optional.of(lugar));
        when(eRepository.save(any(Evento.class))).thenReturn(evento);

        Evento resultado = eService.crearEvento(evento, lugarId);

        assertNotNull(resultado);
        assertEquals("Recital de Rock", resultado.getTitulo());
        assertEquals("BORRADOR", resultado.getEstado());
        verify(eRepository, times(1)).save(evento);
    }

    @Test
    void obtenerEventoPorIdDeberiaRetornarEvento() {
        Long eventoId = 1L;
        Evento evento = new Evento();
        evento.setId(eventoId);
        evento.setTitulo("Recital de Rock");

        when(eRepository.findById(eventoId)).thenReturn(Optional.of(evento));

        Optional<Evento> resultado = eService.obtenerEventoPorId(eventoId);

        assertTrue(resultado.isPresent());
        assertEquals(eventoId, resultado.get().getId());
        verify(eRepository, times(1)).findById(eventoId);
    }
}