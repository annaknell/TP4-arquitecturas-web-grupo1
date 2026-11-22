package org.example.recomendacionesservice.service;

import lombok.RequiredArgsConstructor;
import org.example.recomendacionesservice.client.CatalogoClient;
import org.example.recomendacionesservice.dto.EventoCatalogoDto;
import org.example.recomendacionesservice.entity.ClientePreferencias;
import org.example.recomendacionesservice.entity.CompraRegistrada;
import org.example.recomendacionesservice.repository.ClientePreferenciasRepository;
import org.example.recomendacionesservice.repository.CompraRegistradaRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecomendacionMotorService {

    private final CompraRegistradaRepository compraRegistradaRepository;
    private final ClientePreferenciasRepository preferenciasRepository;
    private final CatalogoClient catalogoClient;

    public List<EventoCatalogoDto> recomendarParaCliente(Long clienteId, Long eventoReferenciaId, int limite) {
        Set<Long> yaComprados = compraRegistradaRepository.findByClienteId(clienteId).stream()
                .map(CompraRegistrada::getEventoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> eventosBase = new LinkedHashSet<>(yaComprados);
        if (eventoReferenciaId != null) {
            eventosBase.add(eventoReferenciaId);
        }

        Map<Long, Integer> puntajePorEvento = new HashMap<>();
        if (!eventosBase.isEmpty()) {
            List<CompraRegistrada> comprasEnEventosBase = compraRegistradaRepository.findByEventoIdIn(List.copyOf(eventosBase));
            Set<Long> compradoresSimilares = comprasEnEventosBase.stream()
                    .map(CompraRegistrada::getClienteId)
                    .filter(id -> id != null && !id.equals(clienteId))
                    .collect(Collectors.toSet());

            if (!compradoresSimilares.isEmpty()) {
                for (Long compradorId : compradoresSimilares) {
                    List<CompraRegistrada> comprasDelSimilar = compraRegistradaRepository.findByClienteId(compradorId);
                    for (CompraRegistrada c : comprasDelSimilar) {
                        Long candId = c.getEventoId();
                        if (candId != null && !yaComprados.contains(candId)) {
                            puntajePorEvento.merge(candId, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        List<ClientePreferencias> prefsList = preferenciasRepository.findAllByClienteId(clienteId);
        Set<String> artistasFav = prefsList.stream()
                .map(ClientePreferencias::getArtistasFavoritos)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> ciudadesFav = prefsList.stream()
                .map(ClientePreferencias::getCiudadesFrecuentes)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<EventoCatalogoDto> publicadosNoComprados = catalogoClient.obtenerEventos().stream()
                .filter(Objects::nonNull)
                .filter(EventoCatalogoDto::esPublicado)
                .filter(ev -> ev.id() != null && !yaComprados.contains(ev.id()))
                .toList();

        Map<Long, Integer> scoreFinal = new HashMap<>();
        for (EventoCatalogoDto ev : publicadosNoComprados) {
            int score = puntajePorEvento.getOrDefault(ev.id(), 0);
            if (ev.ciudad() != null && ciudadesFav.contains(ev.ciudad().toLowerCase())) {
                score += 1;
            }
            boolean coincideArtista = ev.nombresParticipantes().stream()
                    .anyMatch(nombre -> artistasFav.contains(nombre.toLowerCase()));
            if (coincideArtista) {
                score += 2;
            }
            scoreFinal.put(ev.id(), score);
        }

        return publicadosNoComprados.stream()
                .sorted(Comparator.<EventoCatalogoDto>comparingInt(ev -> scoreFinal.getOrDefault(ev.id(), 0)).reversed()
                        .thenComparing(EventoCatalogoDto::id))
                .limit(limite > 0 ? limite : 10)
                .toList();
    }
}