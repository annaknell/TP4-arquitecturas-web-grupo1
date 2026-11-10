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

}
