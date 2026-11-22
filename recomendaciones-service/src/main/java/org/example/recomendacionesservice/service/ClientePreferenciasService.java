package org.example.recomendacionesservice.service;

import org.example.recomendacionesservice.entity.ClientePreferencias;
import org.example.recomendacionesservice.repository.ClientePreferenciasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClientePreferenciasService {
    @Autowired
    private ClientePreferenciasRepository cpRepository;

    public List<ClientePreferencias> obtenerPorClienteId(Long clienteId) {
        return cpRepository.findAllByClienteId(clienteId);
    }

    public ClientePreferencias guardarPreferencias(ClientePreferencias preferencias) {
        Optional<ClientePreferencias> existente = cpRepository.findAllByClienteId(preferencias.getClienteId())
                .stream()
                .findFirst();

        if (existente.isPresent()) {
            // Si ya existe, le asignamos el ID actual para que MongoDB haga un UPDATE en vez de un INSERT duplicado
            preferencias.setId(existente.get().getId());
        }

        return cpRepository.save(preferencias);
    }
}