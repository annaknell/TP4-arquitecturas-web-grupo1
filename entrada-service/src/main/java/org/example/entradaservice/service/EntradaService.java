package org.example.entradaservice.service;

import org.example.entradaservice.entity.Entrada;
import org.example.entradaservice.repository.EntradaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EntradaService {
    @Autowired
    private EntradaRepository entradaRepository;

    public Entrada emitirEntrada(Entrada nuevaEntrada){
        if (nuevaEntrada.getCodigoQr() == null || nuevaEntrada.getCodigoQr().isEmpty()) {
            nuevaEntrada.setCodigoQr("QR-" + UUID.randomUUID().toString().toUpperCase());
        }
        nuevaEntrada.setEstado("EMITIDA");
        return entradaRepository.save(nuevaEntrada);
    }

    public Entrada obtenerPorId(Long id){
        return entradaRepository.findById(id).orElseThrow(() -> new RuntimeException("entrada no encontrada con ID: " + id));
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

}
