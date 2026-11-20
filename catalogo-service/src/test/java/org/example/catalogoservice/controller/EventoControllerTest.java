package org.example.catalogoservice.controller;

import org.example.catalogoservice.entity.Evento;
import org.example.catalogoservice.service.EventoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventoController.class)
class EventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventoService eventoService;

    @Test
    void obtenerEventoDeberiaRetornarEventoOk() throws Exception {
        Long eventoId = 1L;
        Evento evento = new Evento();
        evento.setId(eventoId);
        evento.setTitulo("Recital de Rock");

        when(eventoService.obtenerEventoPorId(eventoId)).thenReturn(Optional.of(evento));

        mockMvc.perform(get("/api/eventos/{id}", eventoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Recital de Rock"));
    }
}