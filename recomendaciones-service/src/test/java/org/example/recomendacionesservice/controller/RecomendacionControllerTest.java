package org.example.recomendacionesservice.controller;

import org.example.recomendacionesservice.entity.Recomendacion;
import org.example.recomendacionesservice.service.RecomendacionMotorService;
import org.example.recomendacionesservice.service.RecomendacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecomendacionController.class)
public class RecomendacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecomendacionService rService;

    @MockitoBean
    private RecomendacionMotorService recomendacionMotorService;

    @Test
    void obtenerPorEventoIdDeberiaRetornarRecomendacion() throws Exception {
        Long eventoId = 10L;
        Recomendacion recomendacion = new Recomendacion(
                "rec123",
                eventoId,
                List.of("Recital Rock", "Festival Indie"),
                LocalDate.now()
        );

        when(rService.obtenerRecomendacionesPorEvento(eventoId)).thenReturn(Optional.of(recomendacion));

        mockMvc.perform(get("/api/recomendaciones/evento/{eventoId}", eventoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoId").value(10))
                .andExpect(jsonPath("$.eventosSugeridos[0]").value("Recital Rock"));
    }
}