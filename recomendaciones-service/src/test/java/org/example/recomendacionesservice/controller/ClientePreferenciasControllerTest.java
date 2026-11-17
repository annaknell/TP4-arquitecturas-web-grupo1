package org.example.recomendacionesservice.controller;

import org.example.recomendacionesservice.entity.ClientePreferencias;
import org.example.recomendacionesservice.service.ClientePreferenciasService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientePreferenciasController.class)
public class ClientePreferenciasControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientePreferenciasService pService;

    @Test
    void obtenerPorClienteIdDeberiaRetornarPreferencias() throws Exception{
        Long clienteId = 1L;
        ClientePreferencias preferencias = new ClientePreferencias(
                "123",
                clienteId,
                List.of("recital"),
                List.of("Banda A"),
                List.of("Tandil")
        );

        when(pService.obtenerPorClienteId(clienteId)).thenReturn(Optional.of(preferencias));
        mockMvc.perform(get("/api/preferencias/cliente/{clienteId}", clienteId)).andExpect(status().isOk()).andExpect(jsonPath("$.clienteId").value(1)).andExpect(jsonPath("$.ciudadesFrecuentes[0]").value("Tandil"));
    }
}
