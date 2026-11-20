package org.example.catalogoservice.service;

import org.example.catalogoservice.entity.Evento;
import org.example.catalogoservice.entity.EventoSector;
import org.example.catalogoservice.entity.Lugar;
import org.example.catalogoservice.dto.EventoNotificacionResponse;
import org.example.catalogoservice.repository.EventoRepository;
import org.example.catalogoservice.repository.EventoSectorRepository;
import org.example.catalogoservice.repository.LugarRepository;
import org.example.catalogoservice.repository.ParticipanteRepository;
import org.example.catalogoservice.client.EntradaClient;
import org.example.catalogoservice.client.FilaVirtualClient;
import org.example.catalogoservice.client.PagosClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    private EntradaClient entradaClient;

    @Autowired
    private PagosClient pagosClient;

    @Autowired
    private FilaVirtualClient filaVirtualClient;

    public List<Evento> listarEventosPublicados() {
        return eventoRepository.findByEstado("PUBLICADO");
    }

    public Optional<Evento> obtenerEventoPorId(Long id) {
        return eventoRepository.findById(id);
    }

    public List<EventoSector> obtenerSectoresYPreciosPorEvento(Long eventoId) {
        return eventoSectorRepository.findByEventoId(eventoId);
    }

    @Transactional(readOnly = true)
    public EventoNotificacionResponse obtenerParaNotificacion(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + eventoId));
        Map<Long, String> sectores = eventoSectorRepository.findByEventoId(eventoId).stream()
                .collect(Collectors.toMap(
                        eventoSector -> eventoSector.getSector().getId(),
                        eventoSector -> eventoSector.getSector().getNombre()
                ));
        return new EventoNotificacionResponse(evento.getId(), evento.getTitulo(), evento.getFechaInicio(),
                evento.getLugar().getNombre(), sectores);
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
        if (eventoPublicado.isAltaDemanda()) {
            filaVirtualClient.abrirFila(eventoPublicado.getId());
        }

        return eventoPublicado;
    }

    @Transactional
    public Evento cancelarEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));

        if ("CANCELADO".equals(evento.getEstado())) {
            return evento;
        }

        List<Long> comprasIds = entradaClient.cancelarEntradasPorEvento(id);
        pagosClient.registrarPendientesPorCancelacion(id, comprasIds);
        evento.setEstado("CANCELADO");
        Evento eventoCancelado = eventoRepository.save(evento);
        if (eventoCancelado.isAltaDemanda()) {
            filaVirtualClient.cancelarFila(eventoCancelado.getId());
        }
        return eventoCancelado;
    }

    @Transactional
    public Evento suspenderEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));

        if ("CANCELADO".equals(evento.getEstado())) {
            throw new IllegalStateException("No se puede suspender un evento cancelado");
        }
        if ("SUSPENDIDO".equals(evento.getEstado())) {
            return evento;
        }

        evento.setEstado("SUSPENDIDO");
        Evento eventoSuspendido = eventoRepository.save(evento);
        if (eventoSuspendido.isAltaDemanda()) {
            filaVirtualClient.pausarFila(eventoSuspendido.getId());
        }
        return eventoSuspendido;
    }

    @Transactional
    public Evento reanudarEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));

        if (!"SUSPENDIDO".equals(evento.getEstado())) {
            throw new IllegalStateException("Solo se puede reanudar un evento suspendido");
        }

        evento.setEstado("PUBLICADO");
        Evento eventoReanudado = eventoRepository.save(evento);
        if (eventoReanudado.isAltaDemanda()) {
            filaVirtualClient.reabrirFila(eventoReanudado.getId());
        }
        return eventoReanudado;
    }

    private void sincronizarInventario(Evento evento) {
        // TODO: Implementar llamada HTTP con RestTemplate o WebClient hacia inventario-service
        // Ejemplo: restTemplate.postForEntity("http://localhost:8083/api/inventario/inicializar", evento, Void.class);
        System.out.println(">>> Sincronizando inventario para el evento publicado ID: " + evento.getId());
    }
}
