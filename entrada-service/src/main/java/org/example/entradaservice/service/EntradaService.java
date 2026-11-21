package org.example.entradaservice.service;

import org.example.entradaservice.dto.ValidarEntradaResponse;
import org.example.entradaservice.entity.Entrada;
import org.example.entradaservice.entity.SolicitudDevolucion;
import org.example.entradaservice.repository.EntradaRepository;
import org.example.entradaservice.repository.SolicitudDevolucionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class EntradaService {
    @Autowired
    private EntradaRepository entradaRepository;

    @Autowired
    private SolicitudDevolucionRepository devolucionRepository;

    @Value("${catalogo-service.url:http://localhost:8082}")
    private String catalogoUrl = "http://localhost:8082";

    @Value("${compra-service.url:http://localhost:8084}")
    private String compraUrl = "http://localhost:8084";

    @Value("${pago-service.url:http://localhost:8085}")
    private String pagoUrl = "http://localhost:8085";

    @Value("${inventario-service.url:http://localhost:8083}")
    private String inventarioUrl = "http://localhost:8083";

    public List<Entrada> emitirEntradas(List<Entrada> nuevasEntradas) {
        for (Entrada nuevaEntrada : nuevasEntradas) {
            if (nuevaEntrada.getCodigoQr() == null || nuevaEntrada.getCodigoQr().isEmpty()) {
                nuevaEntrada.setCodigoQr("QR-" + UUID.randomUUID().toString().toUpperCase());
            }

            if (nuevaEntrada.getSectorId() == null) {
                nuevaEntrada.setSectorId(1L);
            }
            nuevaEntrada.setEstado("EMITIDA");
        }
        return entradaRepository.saveAll(nuevasEntradas);
    }

    public Entrada obtenerPorId(Long id){
        return entradaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "entrada no encontrada con ID: " + id));
    }

    public List<Entrada> listarPorCliente(Long clienteId){
        return entradaRepository.findByClienteId(clienteId);
    }

    public Entrada cancelarEntrada(Long id){
        Entrada e = obtenerPorId(id);
        e.setEstado("CANCELADA");
        return entradaRepository.save(e);
    }

    /**
     * Cancela las entradas emitidas de un evento y devuelve las compras afectadas.
     * Una segunda ejecución no modifica entradas ya canceladas ni devuelve compras nuevas.
     */
    @org.springframework.transaction.annotation.Transactional
    public List<Long> cancelarEntradasPorEvento(Long eventoId) {
        List<Entrada> entradas = entradaRepository.findByEventoIdAndEstadoIn(
                eventoId, List.of("EMITIDA", "CANCELADA"));
        entradas.stream()
                .filter(entrada -> "EMITIDA".equals(entrada.getEstado()))
                .forEach(entrada -> entrada.setEstado("CANCELADA"));
        entradaRepository.saveAll(entradas);
        return entradas.stream().map(Entrada::getCompraId).distinct().toList();
    }

    /**
     * Valida el QR en la puerta: el cambio a USADA lo decide la base en un solo UPDATE,
     * así dos lectores simultáneos no pueden autorizar la misma entrada.
     */
    @org.springframework.transaction.annotation.Transactional
    public ValidarEntradaResponse validar(String codigoQr, Long eventoId) {
        Entrada entrada = entradaRepository.findByCodigoQr(codigoQr).orElse(null);
        if (entrada == null) {
            return rechazo("INEXISTENTE", null);
        }
        if (!Objects.equals(entrada.getEventoId(), eventoId)) {
            return rechazo("EVENTO_EQUIVOCADO", null);
        }

        Instant ahora = Instant.now();
        if (entradaRepository.marcarUsada(codigoQr, ahora) == 1) {
            return new ValidarEntradaResponse(true, null, entrada.getSectorId(), entrada.getAsientoId(), ahora);
        }

        Entrada actual = entradaRepository.findByCodigoQr(codigoQr).orElse(null);
        if (actual == null) {
            return rechazo("INEXISTENTE", null);
        }
        if ("USADA".equals(actual.getEstado())) {
            return rechazo("YA_USADA", actual.getUsadaEn());
        }
        return rechazo(actual.getEstado(), null);
    }

    private ValidarEntradaResponse rechazo(String motivo, Instant usadaEn) {
        return new ValidarEntradaResponse(false, motivo, null, null, usadaEn);
    }

    @org.springframework.transaction.annotation.Transactional
    public SolicitudDevolucion solicitarDevolucion(Long entradaId, Long clienteId) {
        Entrada entrada = obtenerPorId(entradaId);
        if (!Objects.equals(entrada.getClienteId(), clienteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La entrada no pertenece al cliente");
        }
        if (!"EMITIDA".equals(entrada.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada no está en condiciones de devolverse");
        }
        if (devolucionRepository.existsByEntradaIdAndEstadoIn(entradaId,
                List.of(SolicitudDevolucion.Estado.PENDIENTE, SolicitudDevolucion.Estado.APROBADA))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya hay una devolución en curso para esa entrada");
        }
        LocalDateTime inicio = fechaEvento(entrada.getEventoId());
        if (!inicio.isAfter(LocalDateTime.now().plusHours(72))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Faltan 72 horas o menos para el evento");
        }

        SolicitudDevolucion solicitud = new SolicitudDevolucion();
        solicitud.setEntradaId(entradaId);
        solicitud.setClienteId(clienteId);
        solicitud.setEstado(SolicitudDevolucion.Estado.PENDIENTE);
        solicitud.setSolicitadaEn(Instant.now());
        return devolucionRepository.save(solicitud);
    }

    public List<SolicitudDevolucion> listarDevoluciones(String estado) {
        return estado == null || estado.isBlank()
                ? devolucionRepository.findAll()
                : devolucionRepository.findByEstado(SolicitudDevolucion.Estado.valueOf(estado.toUpperCase()));
    }

    @org.springframework.transaction.annotation.Transactional
    public SolicitudDevolucion resolverDevolucion(Long solicitudId, boolean aprobar, String motivoRechazo) {
        SolicitudDevolucion solicitud = devolucionRepository.findById(solicitudId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "solicitud de devolución no encontrada con ID: " + solicitudId));
        if (solicitud.getEstado() != SolicitudDevolucion.Estado.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya fue resuelta");
        }

        if (!aprobar) {
            solicitud.setEstado(SolicitudDevolucion.Estado.RECHAZADA);
            solicitud.setMotivoRechazo(motivoRechazo);
            solicitud.setResueltaEn(Instant.now());
            return devolucionRepository.save(solicitud);
        }

        Entrada entrada = obtenerPorId(solicitud.getEntradaId());
        Map<String, Object> compra = obtenerCompra(entrada.getCompraId());
        int monto = montoPorEntrada(compra);
        reintegrar(solicitudId, ((Number) compra.get("pagoId")).longValue(), monto, entrada);
        liberarLugar(entrada);
        entrada.setEstado("REINTEGRADA");
        entradaRepository.save(entrada);

        solicitud.setEstado(SolicitudDevolucion.Estado.APROBADA);
        solicitud.setResueltaEn(Instant.now());
        return devolucionRepository.save(solicitud);
    }

    private LocalDateTime fechaEvento(Long eventoId) {
        Map<String, Object> evento = RestClient.create(catalogoUrl).get()
                .uri("/api/eventos/{id}", eventoId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });
        Object fecha = evento == null ? null : evento.get("fechaInicio");
        if (fecha == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El evento no tiene fecha de inicio");
        }
        return LocalDateTime.parse(fecha.toString());
    }

    private Map<String, Object> obtenerCompra(Long compraId) {
        Map<String, Object> compra = RestClient.create(compraUrl).get()
                .uri("/api/compras/{id}", compraId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });
        if (compra == null || compra.get("pagoId") == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La compra no tiene un pago asociado");
        }
        return compra;
    }

    private int montoPorEntrada(Map<String, Object> compra) {
        double total = ((Number) compra.get("precioTotal")).doubleValue();
        Object asientos = compra.get("asientos");
        int cantidad = asientos instanceof List<?> lista && !lista.isEmpty() ? lista.size() : 1;
        return (int) Math.round(total / cantidad);
    }

    private void reintegrar(Long solicitudId, Long pagoId, int monto, Entrada entrada) {
        Map<String, Object> asignacion = new LinkedHashMap<>();
        asignacion.put("entradaId", entrada.getId());
        asignacion.put("eventoId", entrada.getEventoId());
        asignacion.put("sectorId", entrada.getSectorId());
        asignacion.put("importeCentavos", (long) monto * 100L);

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("solicitudDevolucionId", solicitudId);
        cuerpo.put("pagoId", pagoId);
        cuerpo.put("monto", monto);
        cuerpo.put("asignaciones", List.of(asignacion));

        RestClient.create(pagoUrl).post()
                .uri("/reintegros/devolucion-aprobada")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }

    private void liberarLugar(Entrada entrada) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("eventId", entrada.getEventoId());
        cuerpo.put("sectorId", entrada.getSectorId());
        if (entrada.getAsientoId() != null) {
            cuerpo.put("asientos", List.of(entrada.getAsientoId()));
        } else {
            cuerpo.put("cantidad", 1);
        }

        RestClient.create(inventarioUrl).post()
                .uri("/api/inventario/liberaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }

}
