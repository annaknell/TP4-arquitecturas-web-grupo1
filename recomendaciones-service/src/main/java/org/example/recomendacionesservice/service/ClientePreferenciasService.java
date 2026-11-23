package org.example.recomendacionesservice.service;

import org.example.recomendacionesservice.dto.DestinatarioNotificacionResponse;
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
            preferencias.setId(existente.get().getId());
        }

        return cpRepository.save(preferencias);
    }

    public List<DestinatarioNotificacionResponse> buscarDestinatarios(String artista, String ciudad) {
        String artistaBuscado = artista == null ? "" : artista.trim().toLowerCase();
        String ciudadBuscada = ciudad == null ? "" : ciudad.trim().toLowerCase();

        return cpRepository.findAll().stream()
                .filter(pref -> pref.getArtistasFavoritos() != null && pref.getArtistasFavoritos().stream()
                        .anyMatch(a -> a != null && a.trim().toLowerCase().equals(artistaBuscado)))
                .filter(pref -> pref.getCiudadesFrecuentes() != null && pref.getCiudadesFrecuentes().stream()
                        .anyMatch(c -> c != null && c.trim().toLowerCase().equals(ciudadBuscada)))
                .map(pref -> new DestinatarioNotificacionResponse(pref.getClienteId(), pref.getEmail()))
                .toList();
    }
}