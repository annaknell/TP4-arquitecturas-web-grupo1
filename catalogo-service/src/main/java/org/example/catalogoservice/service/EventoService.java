package org.example.catalogoservice.service;

import org.example.catalogoservice.entity.Evento;
import org.example.catalogoservice.entity.EventoSector;
import org.example.catalogoservice.entity.Lugar;
import org.example.catalogoservice.repository.EventoRepository;
import org.example.catalogoservice.repository.EventoSectorRepository;
import org.example.catalogoservice.repository.LugarRepository;
import org.example.catalogoservice.repository.ParticipanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EventoService {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private LugarRepository lugarRepository;

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private EventoSectorRepository eventoSectorRepository;

    public List<Evento> listarEventosPublicados() {
        return eventoRepository.findByEstado("PUBLICADO");
    }

    public Optional<Evento> obtenerEventoPorId(Long id) {
        return eventoRepository.findById(id);
    }

    public List<EventoSector> obtenerSectoresYPreciosPorEvento(Long eventoId) {
        return eventoSectorRepository.findByEventoId(eventoId);
    }

    @Transactional
    public Evento crearEvento(Evento evento, Long lugarId) {
        Lugar lugar = lugarRepository.findById(lugarId)
                .orElseThrow(() -> new RuntimeException("Lugar no encontrado con ID: " + lugarId));

        evento.setLugar(lugar);
        evento.setEstado("BORRADOR");

        return eventoRepository.save(evento);
    }

    @Transactional
    public Evento publicarEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));

        evento.setEstado("PUBLICADO");
        Evento eventoPublicado = eventoRepository.save(evento);

        sincronizarInventario(eventoPublicado);

        return eventoPublicado;
    }

    private void sincronizarInventario(Evento evento) {
        // TODO: Implementar llamada HTTP con RestTemplate o WebClient hacia inventario-service
        // Ejemplo: restTemplate.postForEntity("http://localhost:8083/api/inventario/inicializar", evento, Void.class);
        System.out.println(">>> Sincronizando inventario para el evento publicado ID: " + evento.getId());
    }
}