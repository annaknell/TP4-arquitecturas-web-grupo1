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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        lugar.setLatitud(-37.3217);
        lugar.setLongitud(-59.1332);

        when(lRepository.findAll()).thenReturn(List.of(lugar));

        mockMvc.perform(get("/api/lugares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Anfiteatro Municipal"))
                .andExpect(jsonPath("$[0].latitud").value(-37.3217))
                .andExpect(jsonPath("$[0].longitud").value(-59.1332));
    }

    @Test
    void crearLugarDeberiaRetornarCreated() throws Exception {
        Lugar lugar = new Lugar();
        lugar.setId(1L);
        lugar.setNombre("Anfiteatro Municipal");
        lugar.setLatitud(-37.3217);
        lugar.setLongitud(-59.1332);

        when(lRepository.save(any(Lugar.class))).thenReturn(lugar);

        String body = """
                {
                  "nombre": "Anfiteatro Municipal",
                  "latitud": -37.3217,
                  "longitud": -59.1332
                }
                """;

        mockMvc.perform(post("/api/lugares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Anfiteatro Municipal"))
                .andExpect(jsonPath("$.latitud").value(-37.3217))
                .andExpect(jsonPath("$.longitud").value(-59.1332));
    }

    @Test
    void crearLugarSinCoordenadasDeberiaRetornarBadRequest() throws Exception {
        mockMvc.perform(post("/api/lugares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Anfiteatro Municipal\"}"))
                .andExpect(status().isBadRequest());
    }
}