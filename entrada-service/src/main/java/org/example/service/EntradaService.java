package org.example.service;

import org.example.entity.Cliente;
import org.example.entity.Entrada;
import org.example.repository.ClienteRepository;
import org.example.repository.EntradaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Service
public class EntradaService {
    @Autowired
    private EntradaRepository entradaRepository;
    @Autowired
    private ClienteRepository clienteRepository;

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
        Cliente c = clienteRepository.findById(clienteId).orElseThrow(() -> new RuntimeException("cliente no encontrado con ID: " + clienteId));
        return entradaRepository.findByClienteId(c.getId());
    }

    public Entrada cancelarEntrada(Long id){
        Entrada e = entradaRepository.findById(id).orElseThrow(() -> new RuntimeException("entrada no encontrada con ID: " + id));
        e.setEstado("CANCELADA");
        return entradaRepository.save(e);
    }

}
