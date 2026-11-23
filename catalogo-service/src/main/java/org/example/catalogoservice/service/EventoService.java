package org.example.catalogoservice.service;

import org.example.catalogoservice.client.EntradaClient;
import org.example.catalogoservice.client.FilaVirtualClient;
import org.example.catalogoservice.client.PagosClient;
import org.example.catalogoservice.dto.AsignarSectorEventoDto;
import org.example.catalogoservice.dto.EventoModificacionRequest;
import org.example.catalogoservice.dto.EventoNotificacionResponse;
import org.example.catalogoservice.entity.Evento;
import org.example.catalogoservice.entity.EventoSector;
import org.example.catalogoservice.entity.Lugar;
import org.example.catalogoservice.entity.Sector;
import org.example.catalogoservice.messaging.EventoNotificacionPublisher;
import org.example.catalogoservice.messaging.ReporteEventoPublisher;
import org.example.catalogoservice.repository.EventoRepository;
import org.example.catalogoservice.repository.EventoSectorRepository;
import org.example.catalogoservice.repository.LugarRepository;
import org.example.catalogoservice.repository.SectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventoService {

    private record UbicacionAsiento(Long asientoId, String fila, String numero) {
    }

    private record SectorInventario(Long sectorId, String tipo, Integer capacidad, List<UbicacionAsiento> asientos) {
    }

    private record PedidoInventario(Long eventId, List<SectorInventario> sectores) {
    }

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private LugarRepository lugarRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private EventoSectorRepository eventoSectorRepository;

    @Autowired
    private EntradaClient entradaClient;

    @Autowired
    private PagosClient pagosClient;

    @Autowired
    private FilaVirtualClient filaVirtualClient;

    @Autowired
    private EventoNotificacionPublisher eventoNotificacionPublisher;

    @Value("${inventario-service.url:http://localhost:8083}")
    private String inventarioUrl = "http://localhost:8083";

    @Autowired
    private ReporteEventoPublisher reporteEventoPublisher;

    @Cacheable("eventos-listado")
    public List<Evento> listarEventosPublicados() {
        return eventoRepository.findByEstado("PUBLICADO");
    }

    @Cacheable(value = "evento", key = "#id")
    public Optional<Evento> obtenerEventoPorId(Long id) {
        return eventoRepository.findById(id);
    }

    @Cacheable(value = "eventos-busqueda",
            key = "#texto + '|' + #ciudad + '|' + #participante + '|' + #organizadorId + '|' + #desde + '|' + #hasta + '|' + #orden")
    @Transactional(readOnly = true)
    public List<Evento> buscar(String texto, String ciudad, String participante, Long organizadorId,
                               LocalDateTime desde, LocalDateTime hasta, String orden) {
        List<Evento> eventos = eventoRepository.buscar(textoVacio(texto), textoVacio(ciudad),
                textoVacio(participante), organizadorId, desde, hasta);
        if (orden == null || orden.isBlank()) {
            return eventos;
        }
        return eventos.stream().sorted(comparador(orden, texto)).toList();
    }

    private Comparator<Evento> comparador(String orden, String texto) {
        Comparator<LocalDateTime> porFecha = Comparator.nullsLast(Comparator.naturalOrder());
        return switch (orden.toLowerCase()) {
            case "fecha" -> Comparator.comparing(Evento::getFechaInicio, porFecha);
            case "precio" -> {
                Map<Long, Double> minimos = preciosMinimos();
                yield Comparator.comparing(evento -> minimos.getOrDefault(evento.getId(), Double.MAX_VALUE));
            }
            case "relevancia" -> {
                Comparator<Evento> porCoincidencia = Comparator.comparing(
                        (Evento evento) -> coincide(evento, texto) ? 1 : 0, Comparator.reverseOrder());
                yield porCoincidencia.thenComparing(Evento::getFechaInicio, porFecha);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Orden inválido: usar fecha, precio o relevancia");
        };
    }

    private Map<Long, Double> preciosMinimos() {
        Map<Long, Double> minimos = new HashMap<>();
        for (EventoSector eventoSector : eventoSectorRepository.findAll()) {
            minimos.merge(eventoSector.getEvento().getId(), eventoSector.getPrecio(), Math::min);
        }
        return minimos;
    }

    private boolean coincide(Evento evento, String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        String buscado = texto.toLowerCase();
        boolean enTitulo = evento.getTitulo() != null && evento.getTitulo().toLowerCase().contains(buscado);
        boolean enParticipantes = evento.getParticipantes() != null && evento.getParticipantes().stream()
                .anyMatch(participante -> participante.getNombre() != null
                        && participante.getNombre().toLowerCase().contains(buscado));
        return enTitulo || enParticipantes;
    }

    private String textoVacio(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    public List<EventoSector> obtenerSectoresYPreciosPorEvento(Long eventoId) {
        return eventoSectorRepository.findByEventoId(eventoId);
    }

    @Transactional(readOnly = true)
    public EventoNotificacionResponse obtenerParaNotificacion(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento no encontrado con ID: " + eventoId));
        Map<Long, String> sectores = eventoSectorRepository.findByEventoId(eventoId).stream()
                .collect(Collectors.toMap(
                        eventoSector -> eventoSector.getSector().getId(),
                        eventoSector -> eventoSector.getSector().getNombre()
                ));
        return new EventoNotificacionResponse(evento.getId(), evento.getTitulo(), evento.getFechaInicio(),
                evento.getLugar().getNombre(), sectores);
    }

    @CacheEvict(value = {"evento", "eventos-listado", "eventos-busqueda"}, allEntries = true)
    @Transactional
    public Evento crearEvento(Evento evento, Long lugarId) {
        Lugar lugar = lugarRepository.findById(lugarId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lugar no encontrado con ID: " + lugarId));

        evento.setLugar(lugar);
        evento.setEstado("BORRADOR");
        if (evento.getAltaDemanda() == null) {
            evento.setAltaDemanda(false);
        }

        return eventoRepository.save(evento);
    }

    @CacheEvict(value = {"evento", "eventos-listado", "eventos-busqueda"}, allEntries = true)
    @Transactional
    public Evento publicarEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento no encontrado con ID: " + id));

        if ("PUBLICADO".equals(evento.getEstado())) {
            return evento;
        }

        evento.setEstado("PUBLICADO");
        Evento eventoPublicado = eventoRepository.save(evento);
        eventoNotificacionPublisher.publicarEvento(eventoPublicado, UUID.randomUUID().toString());
        reporteEventoPublisher.publicarConfiguracion(eventoPublicado,
                eventoSectorRepository.findByEventoId(id), UUID.randomUUID().toString());

        sincronizarInventario(eventoPublicado);
        if (Boolean.TRUE.equals(eventoPublicado.getAltaDemanda())) {
            filaVirtualClient.abrirFila(eventoPublicado.getId());
        }

        return eventoPublicado;
    }

    @CacheEvict(value = {"evento", "eventos-listado", "eventos-busqueda"}, allEntries = true)
    @Transactional
    public Evento cancelarEvento(Long id) {
        return cancelarEvento(id, UUID.randomUUID().toString());
    }

    @CacheEvict(value = {"evento", "eventos-listado", "eventos-busqueda"}, allEntries = true)
    @Transactional
    public Evento cancelarEvento(Long id, String correlationId) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento no encontrado con ID: " + id));

        if ("CANCELADO".equals(evento.getEstado())) {
            return evento;
        }

        List<Long> comprasIds = entradaClient.cancelarEntradasPorEvento(id);
        pagosClient.registrarPendientesPorCancelacion(id, comprasIds);
        evento.setEstado("CANCELADO");
        Evento eventoCancelado = eventoRepository.save(evento);
        cambiarEstadoInventario(id, "CANCELADO");
        if (Boolean.TRUE.equals(eventoCancelado.getAltaDemanda())) {
            filaVirtualClient.cancelarFila(eventoCancelado.getId());
        }
        eventoNotificacionPublisher.publicarCancelacion(eventoCancelado, correlationId);
        return eventoCancelado;
    }

    @CacheEvict(value = {"evento", "eventos-listado", "eventos-busqueda"}, allEntries = true)
    @Transactional
    public Evento modificarEvento(Long id, EventoModificacionRequest request, String correlationId) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento no encontrado con ID: " + id));
        LinkedHashMap<String, Object> cambios = new LinkedHashMap<>();
        if (request.fechaInicio() != null && !request.fechaInicio().equals(evento.getFechaInicio())) {
            cambios.put("fechaAnterior", evento.getFechaInicio().toString());
            cambios.put("fechaNueva", request.fechaInicio().toString());
            evento.setFechaInicio(request.fechaInicio());
        }
        if (request.fechaFin() != null && !request.fechaFin().equals(evento.getFechaFin())) {
            cambios.put("fechaFinAnterior", evento.getFechaFin() == null ? null : evento.getFechaFin().toString());
            cambios.put("fechaFinNueva", request.fechaFin().toString());
            evento.setFechaFin(request.fechaFin());
        }
        if (request.lugarId() != null && !request.lugarId().equals(evento.getLugar().getId())) {
            cambios.put("lugarAnterior", evento.getLugar().getNombre());
            Lugar lugarNuevo = lugarRepository.findById(request.lugarId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Lugar no encontrado con ID: " + request.lugarId()));
            evento.setLugar(lugarNuevo);
            cambios.put("lugarNuevo", lugarNuevo.getNombre());
        }
        Evento actualizado = eventoRepository.save(evento);
        if (!cambios.isEmpty()) {
            eventoNotificacionPublisher.publicarModificacion(actualizado, correlationId, cambios);
        }
        return actualizado;
    }

    @CacheEvict(value = {"evento", "eventos-listado", "eventos-busqueda"}, allEntries = true)
    @Transactional
    public Evento suspenderEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento no encontrado con ID: " + id));

        if ("CANCELADO".equals(evento.getEstado())) {
            throw new IllegalStateException("No se puede suspender un evento cancelado");
        }
        if ("SUSPENDIDO".equals(evento.getEstado())) {
            return evento;
        }

        evento.setEstado("SUSPENDIDO");
        Evento eventoSuspendido = eventoRepository.save(evento);
        cambiarEstadoInventario(id, "SUSPENDIDO");
        if (Boolean.TRUE.equals(eventoSuspendido.getAltaDemanda())) {
            filaVirtualClient.pausarFila(eventoSuspendido.getId());
        }
        return eventoSuspendido;
    }

    @CacheEvict(value = {"evento", "eventos-listado", "eventos-busqueda"}, allEntries = true)
    @Transactional
    public Evento reanudarEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento no encontrado con ID: " + id));

        if (!"SUSPENDIDO".equals(evento.getEstado())) {
            throw new IllegalStateException("Solo se puede reanudar un evento suspendido");
        }

        evento.setEstado("PUBLICADO");
        Evento eventoReanudado = eventoRepository.save(evento);
        cambiarEstadoInventario(id, "PUBLICADO");
        if (Boolean.TRUE.equals(eventoReanudado.getAltaDemanda())) {
            filaVirtualClient.reabrirFila(eventoReanudado.getId());
        }
        return eventoReanudado;
    }

    private void sincronizarInventario(Evento evento) {
        List<EventoSector> sectores = eventoSectorRepository.findByEventoId(evento.getId());
        if (sectores.isEmpty()) {
            return;
        }

        List<SectorInventario> detalle = sectores.stream()
                .map(eventoSector -> {
                    Sector sector = eventoSector.getSector();
                    boolean numerado = sector.getAsientos() != null && !sector.getAsientos().isEmpty();
                    List<UbicacionAsiento> asientos = numerado
                            ? sector.getAsientos().stream()
                            .map(asiento -> new UbicacionAsiento(
                                    asiento.getId(),
                                    asiento.getFila(),
                                    String.valueOf(asiento.getNumero())
                            ))
                            .toList()
                            : null;
                    int capacidad = numerado ? sector.getAsientos().size() : sector.getCapacidad();
                    String tipo = numerado ? "NUMERADO" : "NO_NUMERADO";
                    return new SectorInventario(sector.getId(), tipo, capacidad, asientos);
                })
                .toList();

        PedidoInventario pedido = new PedidoInventario(evento.getId(), detalle);

        RestClient.create(inventarioUrl).post()
                .uri("/api/inventario/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(pedido)
                .retrieve()
                .toBodilessEntity();
    }

    private void cambiarEstadoInventario(Long eventoId, String estado) {
        try {
            RestClient.create(inventarioUrl).put()
                    .uri(builder -> builder.path("/api/inventario/eventos/{id}/estado")
                            .queryParam("estado", estado)
                            .build(eventoId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            System.err.println("No se pudo actualizar el estado en inventario para el evento " + eventoId + ": " + e.getMessage());
        }
    }

    @CacheEvict(value = {"evento", "eventos-listado", "eventos-busqueda"}, allEntries = true)
    @Transactional
    public EventoSector asignarSectorYPrecio(Long eventoId, AsignarSectorEventoDto dto) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento no encontrado con ID: " + eventoId));

        Sector sector = sectorRepository.findById(dto.sectorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sector no encontrado con ID: " + dto.sectorId()));

        if (sector.getLugar() == null || !sector.getLugar().getId().equals(evento.getLugar().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El sector " + sector.getId() + " no pertenece al lugar del evento (" + evento.getLugar().getId() + ")");
        }

        Integer capacidad = dto.capacidadDisponible() != null
                ? dto.capacidadDisponible()
                : sector.getCapacidad();

        EventoSector eventoSector = eventoSectorRepository
                .findByEventoIdAndSectorId(eventoId, dto.sectorId())
                .orElseGet(() -> EventoSector.builder()
                        .evento(evento)
                        .sector(sector)
                        .build());

        eventoSector.setPrecio(dto.precio());
        eventoSector.setCapacidadDisponible(capacidad);

        return eventoSectorRepository.save(eventoSector);
    }
}