package org.example.inventory.service;

import org.example.inventory.dto.DisponibilidadDTO;
import org.example.inventory.dto.LiberacionDTO;
import org.example.inventory.dto.ProvisionDTO;
import org.example.inventory.dto.ReservaRequestDTO;
import org.example.inventory.dto.ReservaResponseDTO;
import org.example.inventory.entity.ConfiguracionEvento;
import org.example.inventory.entity.EventoAsiento;
import org.example.inventory.entity.Reserva;
import org.example.inventory.entity.TipoSector;
import org.example.inventory.entity.ZonaCupo;
import org.example.inventory.exception.ConflictoException;
import org.example.inventory.exception.NoEncontradoException;
import org.example.inventory.exception.ReglaDeNegocioException;
import org.example.inventory.repository.ConfiguracionEventoRepository;
import org.example.inventory.repository.EventoAsientoRepository;
import org.example.inventory.repository.ReservaRepository;
import org.example.inventory.repository.ZonaCupoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventarioService {

    private final EventoAsientoRepository asientoRepository;
    private final ZonaCupoRepository zonaRepository;
    private final ReservaRepository reservaRepository;
    private final ConfiguracionEventoRepository configuracionRepository;

    @Value("${inventario.reserva.minutos:10}")
    private long minutosReserva;

    public InventarioService(EventoAsientoRepository asientoRepository,
                             ZonaCupoRepository zonaRepository,
                             ReservaRepository reservaRepository,
                             ConfiguracionEventoRepository configuracionRepository) {
        this.asientoRepository = asientoRepository;
        this.zonaRepository = zonaRepository;
        this.reservaRepository = reservaRepository;
        this.configuracionRepository = configuracionRepository;
    }

    @Transactional(readOnly = true)
    public DisponibilidadDTO obtenerDisponibilidad(Long eventId, Long sectorId) {
        ConfiguracionEvento config = configuracion(eventId);
        Instant ahora = Instant.now();

        List<EventoAsiento> asientos = sectorId == null
                ? asientoRepository.findByEventIdOrderBySectorIdAscAsientoIdAsc(eventId)
                : asientoRepository.findByEventIdAndSectorIdOrderByAsientoIdAsc(eventId, sectorId);

        List<ZonaCupo> zonas = zonaRepository.findByEventId(eventId).stream()
                .filter(zona -> sectorId == null || zona.getSectorId().equals(sectorId))
                .toList();

        List<DisponibilidadDTO.SectorDTO> sectores = new ArrayList<>();

        Map<Long, List<EventoAsiento>> porSector = asientos.stream()
                .collect(Collectors.groupingBy(EventoAsiento::getSectorId, LinkedHashMap::new, Collectors.toList()));

        porSector.forEach((id, lista) -> {
            List<DisponibilidadDTO.AsientoDTO> mapa = lista.stream()
                    .map(a -> new DisponibilidadDTO.AsientoDTO(a.getAsientoId(), a.getFila(), a.getNumero(),
                            a.estadoEfectivo(ahora)))
                    .toList();
            int vendidos = (int) mapa.stream().filter(a -> a.getEstado() == EventoAsiento.Estado.VENDIDO).count();
            int libres = (int) mapa.stream().filter(a -> a.getEstado() == EventoAsiento.Estado.LIBRE).count();
            sectores.add(new DisponibilidadDTO.SectorDTO(id, TipoSector.NUMERADO, mapa.size(), vendidos, libres, mapa));
        });

        for (ZonaCupo zona : zonas) {
            long retenidas = reservaRepository.sumarRetenidasEnZona(eventId, zona.getSectorId(), Reserva.Estado.ACTIVA, ahora);
            int disponibles = (int) Math.max(0, zona.getCapacidad() - zona.getVendidos() - retenidas);
            sectores.add(new DisponibilidadDTO.SectorDTO(zona.getSectorId(), TipoSector.GENERAL,
                    zona.getCapacidad(), zona.getVendidos(), disponibles, null));
        }

        sectores.sort(Comparator.comparing(DisponibilidadDTO.SectorDTO::getSectorId));
        return new DisponibilidadDTO(eventId, config.getEstado(), sectores);
    }

    @Transactional
    public ReservaResponseDTO.Resultado reservar(ReservaRequestDTO pedido) {
        Long clienteId = pedido.getClienteId();
        String claveIdempotencia = pedido.getClaveIdempotencia();
        Instant ahora = Instant.now();
        String clave = (claveIdempotencia == null || claveIdempotencia.isBlank()) ? null : claveIdempotencia.trim();

        if (clave != null) {
            Reserva previa = reservaRepository.findByClaveIdempotencia(clave).orElse(null);
            if (previa != null) {
                return repetida(previa, pedido, clienteId);
            }
        }

        ConfiguracionEvento config = configuracion(pedido.getEventId());
        if (config.getEstado() == ConfiguracionEvento.Estado.AGOTADO) {
            throw new ConflictoException("El evento " + pedido.getEventId() + " está agotado");
        }
        if (!config.getEstado().aceptaReservas()) {
            throw new ConflictoException("El evento " + pedido.getEventId()
                    + " no admite reservas: está " + config.getEstado());
        }

        TipoSector tipo = tipoDeSector(pedido);
        int cantidad = tipo == TipoSector.NUMERADO ? pedido.getAsientos().size() : pedido.getCantidad();
        verificarTope(config, pedido.getEventId(), clienteId, cantidad, ahora);

        return tipo == TipoSector.NUMERADO
                ? reservarAsientos(pedido, clienteId, clave, ahora, cantidad)
                : reservarZona(pedido, clienteId, clave, ahora, cantidad);
    }

    private ReservaResponseDTO.Resultado reservarAsientos(ReservaRequestDTO pedido, Long clienteId, String clave,
                                                          Instant ahora, int cantidad) {
        List<Long> pedidos = pedido.getAsientos().stream().distinct().toList();
        if (pedidos.size() != pedido.getAsientos().size()) {
            throw new ReglaDeNegocioException("Hay asientos repetidos en la selección");
        }

        List<EventoAsiento> encontrados = asientoRepository
                .findByEventIdAndSectorIdAndAsientoIdIn(pedido.getEventId(), pedido.getSectorId(), pedidos);
        if (encontrados.size() != pedidos.size()) {
            List<Long> faltantes = pedidos.stream()
                    .filter(id -> encontrados.stream().noneMatch(a -> a.getAsientoId().equals(id)))
                    .toList();
            throw new NoEncontradoException("No existen en el evento los asientos " + faltantes);
        }

        List<EventoAsiento> ordenados = encontrados.stream()
                .sorted(Comparator.comparing(EventoAsiento::getId))
                .toList();

        Reserva reserva = new Reserva(pedido.getEventId(), pedido.getSectorId(), TipoSector.NUMERADO, clienteId,
                cantidad, pedido.getPrecioUnitario(), ahora.plusSeconds(minutosReserva * 60), clave);
        reservaRepository.saveAndFlush(reserva);

        for (EventoAsiento asiento : ordenados) {
            if (asientoRepository.retener(asiento.getId(), reserva.getId(), reserva.getExpiraEn(), ahora) == 0) {

                throw new ConflictoException("El asiento " + asiento.getAsientoId() + " ya está tomado o vendido");
            }
        }
        return new ReservaResponseDTO.Resultado(respuesta(reserva), true);
    }

    private ReservaResponseDTO.Resultado reservarZona(ReservaRequestDTO pedido, Long clienteId, String clave,
                                                      Instant ahora, int cantidad) {
        if (!zonaRepository.existsByEventIdAndSectorId(pedido.getEventId(), pedido.getSectorId())) {
            throw new NoEncontradoException("El sector " + pedido.getSectorId() + " no tiene cupo en el evento");
        }

        Reserva reserva = new Reserva(pedido.getEventId(), pedido.getSectorId(), TipoSector.GENERAL, clienteId,
                cantidad, pedido.getPrecioUnitario(), ahora.plusSeconds(minutosReserva * 60), clave);
        reservaRepository.saveAndFlush(reserva);

        if (zonaRepository.retenerUnidades(pedido.getEventId(), pedido.getSectorId(), cantidad, ahora) == 0) {
            throw new ConflictoException("No hay cupo suficiente en el sector " + pedido.getSectorId()
                    + " para " + cantidad + " entradas");
        }
        return new ReservaResponseDTO.Resultado(respuesta(reserva), true);
    }

    private ReservaResponseDTO.Resultado repetida(Reserva previa, ReservaRequestDTO pedido, Long clienteId) {
        boolean mismaOperacion = previa.getEventId().equals(pedido.getEventId())
                && previa.getClienteId().equals(clienteId);
        if (!mismaOperacion) {
            throw new ReglaDeNegocioException("La clave de idempotencia ya se usó para otra operación");
        }
        return new ReservaResponseDTO.Resultado(respuesta(previa), false);
    }

    private TipoSector tipoDeSector(ReservaRequestDTO pedido) {
        boolean conAsientos = pedido.getAsientos() != null && !pedido.getAsientos().isEmpty();
        boolean conCantidad = pedido.getCantidad() != null;
        if (conAsientos == conCantidad) {
            throw new ReglaDeNegocioException(
                    "Hay que mandar asientos (sector numerado) o cantidad (zona), pero no las dos cosas");
        }
        return conAsientos ? TipoSector.NUMERADO : TipoSector.GENERAL;
    }

    private void verificarTope(ConfiguracionEvento config, Long eventId, Long clienteId, int cantidad, Instant ahora) {
        long yaTiene = asientoRepository.contarVendidosDeCliente(eventId, clienteId)
                + reservaRepository.sumarConfirmadasGeneralesDeCliente(eventId, clienteId, TipoSector.GENERAL, Reserva.Estado.CONFIRMADA)
                + reservaRepository.sumarActivasDeCliente(eventId, clienteId, Reserva.Estado.ACTIVA, ahora);
        int limite = config.getMaxEntradasPorCliente();
        if (yaTiene + cantidad > limite) {
            throw new ReglaDeNegocioException("El evento permite " + limite
                    + " entradas por cliente y ya tiene " + yaTiene);
        }
    }

    @Transactional
    public ReservaResponseDTO confirmar(UUID reservaId) {
        Instant ahora = Instant.now();
        Reserva reserva = buscarReserva(reservaId);

        if (reserva.getEstado() == Reserva.Estado.CONFIRMADA) {
            return respuesta(reserva);
        }
        if (reserva.getEstado() != Reserva.Estado.ACTIVA) {
            throw new ConflictoException("La reserva está " + reserva.getEstado());
        }
        if (reserva.estaVencida(ahora)) {
            throw new ConflictoException("La reserva venció a las " + reserva.getExpiraEn());
        }

        if (reserva.getTipoSector() == TipoSector.NUMERADO) {
            List<EventoAsiento> asientos = asientoRepository.findByReservaId(reservaId);
            for (EventoAsiento asiento : asientos) {
                if (asientoRepository.vender(asiento.getId(), reservaId, ahora) == 0) {
                    throw new ConflictoException("El lugar se liberó mientras se confirmaba la compra");
                }
            }
        } else if (zonaRepository.venderUnidades(reserva.getEventId(), reserva.getSectorId(),
                reserva.getCantidad(), ahora) == 0) {
            throw new ConflictoException("El lugar se liberó mientras se confirmaba la compra");
        }

        reserva.confirmar();
        reservaRepository.save(reserva);
        return respuesta(reserva);
    }

    @Transactional
    public ReservaResponseDTO liberar(UUID reservaId, String motivo) {
        Instant ahora = Instant.now();
        Reserva reserva = buscarReserva(reservaId);

        if (reserva.getEstado() == Reserva.Estado.LIBERADA || reserva.getEstado() == Reserva.Estado.VENCIDA) {
            return respuesta(reserva);
        }
        if (reserva.getEstado() == Reserva.Estado.CONFIRMADA) {
            throw new ConflictoException("Una reserva confirmada solo se libera con una devolución aprobada");
        }

        liberarLugares(reserva, ahora);
        reserva.liberar(motivo == null || motivo.isBlank() ? "compensación" : motivo);
        reservaRepository.save(reserva);
        return respuesta(reserva);
    }

    private void liberarLugares(Reserva reserva, Instant ahora) {
        if (reserva.getTipoSector() == TipoSector.NUMERADO) {
            for (EventoAsiento asiento : asientoRepository.findByReservaId(reserva.getId())) {
                asientoRepository.liberarRetencion(asiento.getId(), reserva.getId(), ahora);
            }
        } else {
            zonaRepository.liberarUnidades(reserva.getEventId(), reserva.getSectorId(),
                    reserva.getCantidad(), ahora);
        }
    }

    @Transactional
    public ProvisionDTO.Resultado provisionar(ProvisionDTO pedido) {
        ConfiguracionEvento.Estado estado = pedido.getEstado() == null ? ConfiguracionEvento.Estado.PUBLICADO : pedido.getEstado();
        int limite = pedido.getMaxEntradasPorCliente() == null ? 6 : pedido.getMaxEntradasPorCliente();

        ConfiguracionEvento config = configuracionRepository.findById(pedido.getEventId()).orElse(null);
        if (config == null) {
            configuracionRepository.save(new ConfiguracionEvento(pedido.getEventId(), estado, limite));
        } else {
            config.setEstado(estado);
            config.setMaxEntradasPorCliente(limite);
            config.setActualizadoEn(Instant.now());
            configuracionRepository.save(config);
        }

        Set<String> conocidos = new HashSet<>();
        asientoRepository.findByEventIdOrderBySectorIdAscAsientoIdAsc(pedido.getEventId())
                .forEach(a -> conocidos.add(a.getSectorId() + ":" + a.getAsientoId()));

        int asientosCreados = 0;
        int asientosExistentes = 0;
        int zonasCreadas = 0;
        int zonasExistentes = 0;

        for (ProvisionDTO.SectorDef sector : pedido.getSectores()) {
            if (sector.getTipo() == TipoSector.NUMERADO) {
                if (sector.getAsientos() == null || sector.getAsientos().isEmpty()) {
                    throw new ReglaDeNegocioException("El sector numerado " + sector.getSectorId() + " no trae asientos");
                }
                for (ProvisionDTO.AsientoDef asiento : sector.getAsientos()) {
                    if (!conocidos.add(sector.getSectorId() + ":" + asiento.getAsientoId())) {
                        asientosExistentes++;
                        continue;
                    }
                    asientoRepository.save(new EventoAsiento(pedido.getEventId(), sector.getSectorId(),
                            asiento.getAsientoId(), asiento.getFila(), asiento.getNumero()));
                    asientosCreados++;
                }
            } else if (sector.getCapacidad() == null || sector.getCapacidad() <= 0) {
                throw new ReglaDeNegocioException("La zona " + sector.getSectorId() + " no trae una capacidad válida");
            } else if (zonaRepository.existsByEventIdAndSectorId(pedido.getEventId(), sector.getSectorId())) {
                zonasExistentes++;
            } else {
                zonaRepository.save(new ZonaCupo(pedido.getEventId(), sector.getSectorId(), sector.getCapacidad()));
                zonasCreadas++;
            }
        }

        return new ProvisionDTO.Resultado(pedido.getEventId(), asientosCreados, asientosExistentes,
                zonasCreadas, zonasExistentes);
    }

    @Transactional
    public LiberacionDTO.Resultado cambiarEstado(Long eventId, ConfiguracionEvento.Estado estado, Integer maxEntradas) {
        ConfiguracionEvento config = configuracion(eventId);
        config.setEstado(estado);
        if (maxEntradas != null) {
            config.setMaxEntradasPorCliente(maxEntradas);
        }
        config.setActualizadoEn(Instant.now());
        configuracionRepository.save(config);

        return estado == ConfiguracionEvento.Estado.CANCELADO
                ? liberarReservasDelEvento(eventId, "evento cancelado")
                : new LiberacionDTO.Resultado(eventId, 0, 0);
    }

    @Transactional
    private LiberacionDTO.Resultado liberarReservasDelEvento(Long eventId, String motivo) {
        Instant ahora = Instant.now();
        int asientosLiberados = 0;
        int unidadesLiberadas = 0;

        for (Reserva reserva : reservaRepository.findByEventIdAndEstado(eventId, Reserva.Estado.ACTIVA)) {

            if (reservaRepository.liberarSiActiva(reserva.getId(), Reserva.Estado.ACTIVA, Reserva.Estado.LIBERADA, ahora, motivo) == 0) {
                continue;
            }
            if (reserva.getTipoSector() == TipoSector.NUMERADO) {
                for (EventoAsiento asiento : asientoRepository.findByReservaId(reserva.getId())) {
                    asientosLiberados += asientoRepository.liberarRetencion(asiento.getId(), reserva.getId(), ahora);
                }
            } else {
                zonaRepository.liberarUnidades(reserva.getEventId(), reserva.getSectorId(),
                        reserva.getCantidad(), ahora);
                unidadesLiberadas += reserva.getCantidad();
            }
        }
        return new LiberacionDTO.Resultado(eventId, asientosLiberados, unidadesLiberadas);
    }

    @Transactional
    public LiberacionDTO.Resultado liberarVendidos(LiberacionDTO pedido) {
        Instant ahora = Instant.now();
        int asientos = 0;
        int unidades = 0;

        if (pedido.getAsientos() != null) {
            for (Long asientoId : pedido.getAsientos()) {
                asientos += asientoRepository.liberarVendido(pedido.getEventId(), pedido.getSectorId(), asientoId, ahora);
            }
        }

        if (pedido.getCantidad() != null && pedido.getCantidad() > 0) {
            unidades = zonaRepository.devolverVendidas(pedido.getEventId(), pedido.getSectorId(), pedido.getCantidad(), ahora);
        }
        return new LiberacionDTO.Resultado(pedido.getEventId(), asientos, unidades);
    }

    @Scheduled(fixedDelayString = "${inventario.reserva.barrido-ms:5000}")
    @Transactional
    public void barrerVencidas() {
        Instant ahora = Instant.now();
        asientoRepository.liberarRetencionesVencidas(ahora);

        for (Reserva reserva : reservaRepository.findByEstadoAndExpiraEnBefore(Reserva.Estado.ACTIVA, ahora)) {
            if (reservaRepository.vencerSiActiva(reserva.getId(), Reserva.Estado.ACTIVA, Reserva.Estado.VENCIDA) == 1
                    && reserva.getTipoSector() == TipoSector.GENERAL) {
                zonaRepository.liberarUnidades(reserva.getEventId(), reserva.getSectorId(),
                        reserva.getCantidad(), ahora);
            }
        }
    }

    private ConfiguracionEvento configuracion(Long eventId) {
        return configuracionRepository.findById(eventId)
                .orElseThrow(() -> new NoEncontradoException(
                        "El evento " + eventId + " no tiene disponibilidad registrada en inventario"));
    }

    private Reserva buscarReserva(UUID reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new NoEncontradoException("No existe la reserva " + reservaId));
    }

    private ReservaResponseDTO respuesta(Reserva reserva) {
        List<Long> asientos = reserva.getTipoSector() == TipoSector.NUMERADO
                ? asientoRepository.findByReservaId(reserva.getId()).stream()
                        .map(EventoAsiento::getAsientoId).sorted().toList()
                : List.of();
        return ReservaResponseDTO.desde(reserva, asientos);
    }
}
