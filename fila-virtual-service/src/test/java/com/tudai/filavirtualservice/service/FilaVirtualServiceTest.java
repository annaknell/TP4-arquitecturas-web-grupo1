package com.tudai.filavirtualservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyList;

import com.tudai.filavirtualservice.domain.EstadoTicketFila;
import com.tudai.filavirtualservice.dto.IngresoFilaResponse;
import com.tudai.filavirtualservice.dto.PosicionFilaResponse;
import com.tudai.filavirtualservice.exception.EventoNoHabilitadoException;
import com.tudai.filavirtualservice.exception.TicketNoEncontradoException;
import com.tudai.filavirtualservice.redis.FilaVirtualRedisRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FilaVirtualServiceTest {

    @Mock
    private FilaVirtualRedisRepository repository;

    @InjectMocks
    private FilaVirtualService service;

    @Test
    void rechazaIngresoSiCatalogoNoHabilitoElEventoParaFila() {
        when(repository.ingresar(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new FilaVirtualRedisRepository.ResultadoIngreso(false, null, null, 0));

        assertThrows(EventoNoHabilitadoException.class, () -> service.ingresar(10L, 7L));
    }

    @Test
    void reingresoDevuelveElMismoTicketSinCrearOtraPosicion() {
        when(repository.ingresar(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new FilaVirtualRedisRepository.ResultadoIngreso(true, "ticket-existente", "EN_ESPERA", 1_700_000_000_000L));

        IngresoFilaResponse respuesta = service.ingresar(10L, 7L);

        assertEquals("ticket-existente", respuesta.ticketId());
        assertEquals(EstadoTicketFila.EN_ESPERA, respuesta.estado());
        assertTrue(respuesta.reingreso());
    }

    @Test
    void calculaPosicionYPersonasDelanteDesdeElRangoDeRedis() {
        when(repository.buscarTicket(10L, 7L)).thenReturn(Optional.of(
                new FilaVirtualRedisRepository.TicketAlmacenado("ticket-7", "EN_ESPERA", 1_700_000_000_000L, null, null)));
        when(repository.obtenerRango(10L, 7L)).thenReturn(4L);

        PosicionFilaResponse respuesta = service.consultarPosicion(10L, 7L);

        assertEquals(5L, respuesta.posicion());
        assertEquals(4L, respuesta.personasDelante());
        assertEquals(EstadoTicketFila.EN_ESPERA, respuesta.estado());
    }

    @Test
    void ticketHabilitadoNoExponeUnaPosicionEnLaCola() {
        when(repository.buscarTicket(10L, 7L)).thenReturn(Optional.of(
                new FilaVirtualRedisRepository.TicketAlmacenado("ticket-7", "HABILITADO", 1_700_000_000_000L, 1_700_000_600_000L, "token-7")));

        PosicionFilaResponse respuesta = service.consultarPosicion(10L, 7L);

        assertNull(respuesta.posicion());
        assertNull(respuesta.personasDelante());
        assertEquals(EstadoTicketFila.HABILITADO, respuesta.estado());
    }

    @Test
    void noPermiteConsultarUnTicketInexistente() {
        when(repository.buscarTicket(10L, 7L)).thenReturn(Optional.empty());

        assertThrows(TicketNoEncontradoException.class, () -> service.consultarPosicion(10L, 7L));
    }

    @Test
    void habilitaLaSiguienteTandaConAutorizacionesTemporales() {
        when(repository.obtenerTamanioTanda(10L)).thenReturn(2);
        when(repository.habilitarSiguienteTanda(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(2), anyList()))
                .thenReturn(new FilaVirtualRedisRepository.ResultadoTanda(false, java.util.List.of(
                        new FilaVirtualRedisRepository.HabilitacionCreada(7L, "ticket-7", "token-7", 1_700_000_600_000L),
                        new FilaVirtualRedisRepository.HabilitacionCreada(8L, "ticket-8", "token-8", 1_700_000_600_000L))));

        var respuesta = service.habilitarSiguienteTanda(10L);

        assertEquals(2, respuesta.habilitados().size());
        assertEquals(7L, respuesta.habilitados().getFirst().clienteId());
        assertEquals("token-7", respuesta.habilitados().getFirst().token());
    }

    @Test
    void delegaElVencimientoDeAutorizacionesAlRepositorio() {
        when(repository.vencerAutorizacionesPendientes()).thenReturn(2);

        assertEquals(2, service.vencerAutorizacionesPendientes());
    }

    @Test
    void habilitaUnaTandaAutomaticaSoloParaEventosConVentanaVencida() {
        when(repository.eventosConFilaActiva()).thenReturn(Set.of(10L));
        when(repository.obtenerTamanioTanda(10L)).thenReturn(2);
        when(repository.habilitarSiguienteTandaSiCorresponde(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(2), anyList()))
                .thenReturn(new FilaVirtualRedisRepository.ResultadoTanda(false, java.util.List.of(
                        new FilaVirtualRedisRepository.HabilitacionCreada(7L, "ticket-7", "token-7", 1_700_000_600_000L))));

        assertEquals(1, service.habilitarTandasVencidas());
        verify(repository).habilitarSiguienteTandaSiCorresponde(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(2), anyList());
    }

    @Test
    void noCuentaUnaTandaQueAunPermaneceActiva() {
        when(repository.eventosConFilaActiva()).thenReturn(Set.of(10L));
        when(repository.obtenerTamanioTanda(10L)).thenReturn(2);
        when(repository.habilitarSiguienteTandaSiCorresponde(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(2), anyList()))
                .thenReturn(new FilaVirtualRedisRepository.ResultadoTanda(false, java.util.List.of()));

        assertEquals(0, service.habilitarTandasVencidas());
    }
}
