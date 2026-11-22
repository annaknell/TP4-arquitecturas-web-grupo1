package org.example.recomendacionesservice.service;

import org.example.recomendacionesservice.entity.ClientePreferencias;
import org.example.recomendacionesservice.repository.ClientePreferenciasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;
import org.example.recomendacionesservice.dto.DestinatarioNotificacionResponse;

@Service
public class ClientePreferenciasService {
    @Autowired
    private ClientePreferenciasRepository cpRepository;

    public Optional<ClientePreferencias> obtenerPorClienteId(Long clienteId){
        return cpRepository.findByClienteId(clienteId);
    }

    public ClientePreferencias guardarPreferencias(ClientePreferencias preferencias) {
        Optional<ClientePreferencias> existente = cpRepository.findByClienteId(preferencias.getClienteId());

        if (existente.isPresent()) {
            // Si ya existe, le asignamos el ID actual para que MongoDB haga un UPDATE en vez de un INSERT duplicado
            preferencias.setId(existente.get().getId());
        }

        return cpRepository.save(preferencias);
    }

    public List<DestinatarioNotificacionResponse> buscarDestinatarios(String artista, String ciudad) {
        return cpRepository
                .findByArtistasFavoritosContainingIgnoreCaseAndCiudadesFrecuentesContainingIgnoreCase(artista, ciudad)
                .stream()
                .filter(preferencia -> preferencia.getEmail() != null && !preferencia.getEmail().isBlank())
                .map(preferencia -> new DestinatarioNotificacionResponse(
                        preferencia.getClienteId(), preferencia.getEmail()))
                .toList();
    }
}
