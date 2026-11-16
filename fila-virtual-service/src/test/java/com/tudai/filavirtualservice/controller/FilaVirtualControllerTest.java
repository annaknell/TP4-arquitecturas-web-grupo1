package com.tudai.filavirtualservice.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tudai.filavirtualservice.domain.EstadoTicketFila;
import com.tudai.filavirtualservice.dto.IngresoFilaResponse;
import com.tudai.filavirtualservice.dto.PosicionFilaResponse;
import com.tudai.filavirtualservice.exception.EventoNoHabilitadoException;
import com.tudai.filavirtualservice.exception.TicketNoEncontradoException;
import com.tudai.filavirtualservice.service.FilaVirtualService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FilaVirtualControllerTest {

    @Mock
    private FilaVirtualService filaVirtualService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FilaVirtualController(filaVirtualService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void ingresoInicialDevuelveCreatedYUsaElUsuarioDelHeader() throws Exception {
        when(filaVirtualService.ingresar(10L, 7L)).thenReturn(
                new IngresoFilaResponse("ticket-7", EstadoTicketFila.EN_ESPERA, Instant.parse("2026-11-16T12:00:00Z"), false));

        mockMvc.perform(post("/fila/10/ingresos").header("X-User-Id", "7"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value("ticket-7"))
                .andExpect(jsonPath("$.reingreso").value(false));

        verify(filaVirtualService).ingresar(10L, 7L);
    }

    @Test
    void reingresoDevuelveOk() throws Exception {
        when(filaVirtualService.ingresar(10L, 7L)).thenReturn(
                new IngresoFilaResponse("ticket-7", EstadoTicketFila.EN_ESPERA, Instant.parse("2026-11-16T12:00:00Z"), true));

        mockMvc.perform(post("/fila/10/ingresos").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reingreso").value(true));
    }

    @Test
    void consultaPosicionDelUsuarioDelHeader() throws Exception {
        when(filaVirtualService.consultarPosicion(10L, 7L)).thenReturn(
                new PosicionFilaResponse("ticket-7", EstadoTicketFila.EN_ESPERA, 3L, 2L, null, 10));

        mockMvc.perform(get("/fila/10/mi-posicion").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posicion").value(3))
                .andExpect(jsonPath("$.personasDelante").value(2));

        verify(filaVirtualService).consultarPosicion(10L, 7L);
    }

    @Test
    void eventoNoHabilitadoDevuelveConflict() throws Exception {
        when(filaVirtualService.ingresar(10L, 7L)).thenThrow(new EventoNoHabilitadoException());

        mockMvc.perform(post("/fila/10/ingresos").header("X-User-Id", "7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_NO_HABILITADO"));
    }

    @Test
    void ticketInexistenteDevuelveNotFound() throws Exception {
        when(filaVirtualService.consultarPosicion(10L, 7L)).thenThrow(new TicketNoEncontradoException());

        mockMvc.perform(get("/fila/10/mi-posicion").header("X-User-Id", "7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TICKET_NO_ENCONTRADO"));
    }
}
