package org.example.entradaservice.service;

import org.example.entradaservice.dto.ValidarEntradaResponse;
import org.example.entradaservice.entity.Entrada;
import org.example.entradaservice.repository.EntradaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class EntradaService {
    @Autowired
    private EntradaRepository entradaRepository;

    public List<Entrada> emitirEntradas(List<Entrada> nuevasEntradas) {
        for (Entrada nuevaEntrada : nuevasEntradas) {
            if (nuevaEntrada.getCodigoQr() == null || nuevaEntrada.getCodigoQr().isEmpty()) {
                nuevaEntrada.setCodigoQr("QR-" + UUID.randomUUID().toString().toUpperCase());
            }

            if (nuevaEntrada.getSectorId() == null) {
                nuevaEntrada.setSectorId(1L);
            }
            nuevaEntrada.setEstado("EMITIDA");
        }
        return entradaRepository.saveAll(nuevasEntradas);
    }

    public Entrada obtenerPorId(Long id){
        return entradaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "entrada no encontrada con ID: " + id));
    }

    public List<Entrada> listarPorCliente(Long clienteId){
        return entradaRepository.findByClienteId(clienteId);
    }

    public Entrada cancelarEntrada(Long id){
        Entrada e = obtenerPorId(id);
        e.setEstado("CANCELADA");
        return entradaRepository.save(e);
    }

    /**
     * Cancela las entradas emitidas de un evento y devuelve las compras afectadas.
     * Una segunda ejecución no modifica entradas ya canceladas ni devuelve compras nuevas.
     */
    @org.springframework.transaction.annotation.Transactional
    public List<Long> cancelarEntradasPorEvento(Long eventoId) {
        List<Entrada> entradas = entradaRepository.findByEventoIdAndEstadoIn(
                eventoId, List.of("EMITIDA", "CANCELADA"));
        entradas.stream()
                .filter(entrada -> "EMITIDA".equals(entrada.getEstado()))
                .forEach(entrada -> entrada.setEstado("CANCELADA"));
        entradaRepository.saveAll(entradas);
        return entradas.stream().map(Entrada::getCompraId).distinct().toList();
    }

    /**
     * Valida el QR en la puerta: el cambio a USADA lo decide la base en un solo UPDATE,
     * así dos lectores simultáneos no pueden autorizar la misma entrada.
     */
    @org.springframework.transaction.annotation.Transactional
    public ValidarEntradaResponse validar(String codigoQr, Long eventoId) {
        Entrada entrada = entradaRepository.findByCodigoQr(codigoQr).orElse(null);
        if (entrada == null) {
            return rechazo("INEXISTENTE", null);
        }
        if (!Objects.equals(entrada.getEventoId(), eventoId)) {
            return rechazo("EVENTO_EQUIVOCADO", null);
        }

        Instant ahora = Instant.now();
        if (entradaRepository.marcarUsada(codigoQr, ahora) == 1) {
            return new ValidarEntradaResponse(true, null, entrada.getSectorId(), entrada.getAsientoId(), ahora);
        }

        Entrada actual = entradaRepository.findByCodigoQr(codigoQr).orElse(null);
        if (actual == null) {
            return rechazo("INEXISTENTE", null);
        }
        if ("USADA".equals(actual.getEstado())) {
            return rechazo("YA_USADA", actual.getUsadaEn());
        }
        return rechazo(actual.getEstado(), null);
    }

    private ValidarEntradaResponse rechazo(String motivo, Instant usadaEn) {
        return new ValidarEntradaResponse(false, motivo, null, null, usadaEn);
    }

}
