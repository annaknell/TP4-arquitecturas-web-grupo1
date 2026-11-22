package org.example.recomendacionesservice.service;

import org.example.recomendacionesservice.entity.ClientePreferencias;
import org.example.recomendacionesservice.repository.ClientePreferenciasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ClientePreferenciasServiceTest {
    @Mock
    private ClientePreferenciasRepository cpRepository;

    @InjectMocks
    private ClientePreferenciasService cpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void guardarPreferenciasDeberiaGuardarExitosamente() {
        ClientePreferencias preferencias = new ClientePreferencias(
                "123",
                1L,
                List.of("recital"),
                List.of("Banda A"),
                List.of("Tandil")
        );

        when(cpRepository.findAllByClienteId(1L)).thenReturn(List.of());
        when(cpRepository.save(any(ClientePreferencias.class))).thenReturn(preferencias);

        ClientePreferencias resultado = cpService.guardarPreferencias(preferencias);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getClienteId());
        assertEquals(1, resultado.getTiposEventoFavoritos().size());
        verify(cpRepository, times(1)).save(preferencias);
    }

    @Test
    void obtenerPorClienteIdDeberiaRetornarPreferencias() {
        Long clienteId = 1L;
        ClientePreferencias preferencias = new ClientePreferencias(
                "123",
                clienteId,
                List.of("teatro"),
                List.of("Artista B"),
                List.of("Buenos Aires")
        );

        when(cpRepository.findAllByClienteId(clienteId)).thenReturn(List.of(preferencias));

        List<ClientePreferencias> resultado = cpService.obtenerPorClienteId(clienteId);

        assertFalse(resultado.isEmpty());
        assertEquals(clienteId, resultado.get(0).getClienteId());
        verify(cpRepository, times(1)).findAllByClienteId(clienteId);
    }
}