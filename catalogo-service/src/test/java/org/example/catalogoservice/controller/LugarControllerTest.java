package org.example.catalogoservice.controller;

import org.example.catalogoservice.entity.Lugar;
import org.example.catalogoservice.repository.LugarRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.*;

@WebMvcTest(LugarController.class)
class LugarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LugarRepository lRepository;

    @Test
    void listarLugaresDeberiaRetornarListaOk() throws Exception {
        Lugar lugar = new Lugar();
        lugar.setId(1L);
        lugar.setNombre("Anfiteatro Municipal");

        when(lRepository.findAll()).thenReturn(List.of(lugar));

        mockMvc.perform(get("/api/lugares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Anfiteatro Municipal"));
    }

    @Test
    void crearLugarDeberiaRetornarCreated() throws Exception {
        Lugar lugar = new Lugar();
        lugar.setId(1L);
        lugar.setNombre("Anfiteatro Municipal");

        when(lRepository.save(any(Lugar.class))).thenReturn(lugar);

        mockMvc.perform(post("/api/lugares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Anfiteatro Municipal\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Anfiteatro Municipal"));
    }
}