package com.tudai.filavirtualservice.service;

import com.tudai.filavirtualservice.domain.ConfiguracionFilaEvento;
import com.tudai.filavirtualservice.domain.EstadoEventoFila;
import com.tudai.filavirtualservice.domain.EstadoTicketFila;
import com.tudai.filavirtualservice.dto.IngresoFilaResponse;
import com.tudai.filavirtualservice.dto.AutorizacionCompraResponse;
import com.tudai.filavirtualservice.dto.HabilitarTandaResponse;
import com.tudai.filavirtualservice.dto.PosicionFilaResponse;
import com.tudai.filavirtualservice.exception.EventoNoHabilitadoException;
import com.tudai.filavirtualservice.exception.TicketNoEncontradoException;
import com.tudai.filavirtualservice.redis.FilaVirtualRedisRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FilaVirtualService {
    private static final int INTERVALO_CONSULTA_SUGERIDO_SEGUNDOS = 10;
    private final FilaVirtualRedisRepository repository;

    public FilaVirtualService(FilaVirtualRedisRepository repository) {
        this.repository = repository;
    }

    public IngresoFilaResponse ingresar(Long eventoId, Long clienteId) {
        validarIds(eventoId, clienteId);
        FilaVirtualRedisRepository.ResultadoIngreso resultado = repository.ingresar(eventoId, clienteId, UUID.randomUUID().toString());
        if (resultado.eventoNoHabilitado()) {
            throw new EventoNoHabilitadoException();
        }
        return new IngresoFilaResponse(resultado.ticketId(), EstadoTicketFila.valueOf(resultado.estado()),
                Instant.ofEpochMilli(resultado.ingresadoEnEpochMillis()), resultado.reingreso());
    }

    public PosicionFilaResponse consultarPosicion(Long eventoId, Long clienteId) {
        validarIds(eventoId, clienteId);
        FilaVirtualRedisRepository.TicketAlmacenado ticket = repository.buscarTicket(eventoId, clienteId)
                .orElseThrow(TicketNoEncontradoException::new);
        EstadoTicketFila estado = EstadoTicketFila.valueOf(ticket.estado());
        Long posicion = null;
        Long personasDelante = null;
        if (estado == EstadoTicketFila.EN_ESPERA) {
            Long rangoCeroBasado = repository.obtenerRango(eventoId, clienteId);
            if (rangoCeroBasado != null) {
                posicion = rangoCeroBasado + 1;
                personasDelante = rangoCeroBasado;
            }
        }
        return new PosicionFilaResponse(ticket.ticketId(), estado, posicion, personasDelante,
                ticket.habilitadoHastaEpochMillis() == null ? null : Instant.ofEpochMilli(ticket.habilitadoHastaEpochMillis()),
                INTERVALO_CONSULTA_SUGERIDO_SEGUNDOS, ticket.autorizacionToken());
    }

    public void configurarEvento(Long eventoId, int tamanioTanda, int ventanaCompraMinutos) {
        if (eventoId == null || eventoId <= 0) {
            throw new IllegalArgumentException("El identificador del evento debe ser positivo");
        }
        repository.guardarConfiguracion(new ConfiguracionFilaEvento(eventoId, EstadoEventoFila.ABIERTA,
                tamanioTanda, Duration.ofMinutes(ventanaCompraMinutos)));
    }

    public void cambiarEstadoEvento(Long eventoId, EstadoEventoFila estado) {
        if (eventoId == null || eventoId <= 0 || estado == null) {
            throw new IllegalArgumentException("El identificador del evento y el estado son obligatorios");
        }
        repository.actualizarEstado(eventoId, estado);
    }

    /** Habilita estrictamente a los primeros clientes que aún permanecen en la cola. */
    public HabilitarTandaResponse habilitarSiguienteTanda(Long eventoId) {
        if (eventoId == null || eventoId <= 0) {
            throw new IllegalArgumentException("El identificador del evento debe ser positivo");
        }
        int tamanioTanda = repository.obtenerTamanioTanda(eventoId);
        if (tamanioTanda <= 0) {
            throw new EventoNoHabilitadoException();
        }
        List<String> tokens = java.util.stream.IntStream.range(0, tamanioTanda)
                .mapToObj(indice -> UUID.randomUUID().toString())
                .toList();
        FilaVirtualRedisRepository.ResultadoTanda resultado = repository.habilitarSiguienteTanda(eventoId, tamanioTanda, tokens);
        if (resultado.eventoNoHabilitado()) {
            throw new EventoNoHabilitadoException();
        }
        return new HabilitarTandaResponse(resultado.habilitaciones().stream()
                .map(habilitacion -> new AutorizacionCompraResponse(habilitacion.clienteId(), habilitacion.ticketId(),
                        habilitacion.token(), Instant.ofEpochMilli(habilitacion.venceEnEpochMillis())))
                .toList());
    }

    public int vencerAutorizacionesPendientes() {
        return repository.vencerAutorizacionesPendientes();
    }

    private void validarIds(Long eventoId, Long clienteId) {
        if (eventoId == null || eventoId <= 0 || clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("Los identificadores de evento y cliente deben ser positivos");
        }
    }
}
