package org.example.compraservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.example.compraservice.client.*;
import org.example.compraservice.dto.*;
import org.example.compraservice.entity.Compra;
import org.example.compraservice.exception.LimiteEntradasExcedidoException;
import org.example.compraservice.messaging.CompraCompletadaPublisher;
import org.example.compraservice.messaging.ReporteVentaPublisher;
import org.example.compraservice.repository.CompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class CompraService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private InventarioClient inventarioClient;
    @Autowired
    private PagosClient pagosClient;
    @Autowired
    private PagosClientResiliente pagosClientResiliente;
    @Autowired
    private EntradaClient entradaClient;
    @Autowired
    private CompraCompletadaPublisher compraCompletadaPublisher;
    @Autowired
    private ReporteVentaPublisher reporteVentaPublisher;

    @Transactional
    public Compra procesarCompra(CompraRequest request, String idempotencyKey, Long clienteId) {
        return procesarCompra(request, idempotencyKey, clienteId, UUID.randomUUID().toString(), null);
    }

    @Transactional
    public Compra procesarCompra(CompraRequest request, String idempotencyKey, Long clienteId, String correlationId) {
        return procesarCompra(request, idempotencyKey, clienteId, correlationId, null);
    }

    @Transactional
    public Compra procesarCompra(CompraRequest request, String idempotencyKey, Long clienteId,
                                 String correlationId, String email) {

        // 1. Verificar Idempotencia
        Optional<Compra> compraExistente = compraRepository.findByIdempotencyKey(idempotencyKey);
        if (compraExistente.isPresent()) {
            return compraExistente.get();
        }

        // Inicializar la Compra
        Compra compra = new Compra();
        compra.setClienteId(clienteId);
        compra.setEmailCliente(email);
        compra.setEventoId(request.eventoId());
        compra.setIdempotencyKey(idempotencyKey);
        // En zonas no numeradas no hay asientos: se guarda una lista vacía (nunca null)
        // para que la entidad siga siendo persistible y el emisor use `cantidad`.
        compra.setAsientos(request.asientos() == null
                ? new ArrayList<>() : new ArrayList<>(request.asientos()));
        compra.setSectorId(request.sectorId());
        compra.setCantidad(request.cantidad());
        compra.setPrecioTotal(request.precioTotal());
        compra.setEstado("PENDIENTE");
        compra.addPaso("Inicio de compra", "EJECUTADO");
        compraRepository.save(compra);

        try {
            // PASO 1: Reservar en Inventario (10 minutos).
            // US-18: inventario es la única fuente de verdad del tope por cliente
            // (configurable por evento y con reservas activas). Acá no se re-cuenta nada:
            // su 422 se propaga con el detalle para que el cliente sepa cuántas lleva.
            ReservaResponse reserva;
            try {
                reserva = inventarioClient.reservarAsientos(
                        new ReservaRequest(request.eventoId(), clienteId, request.asientos(),
                                request.sectorId(), request.cantidad(),
                                request.autorizacionFila(), idempotencyKey)).getBody();
            } catch (FeignException e) {
                if (e.status() == 422) {
                    throw new LimiteEntradasExcedidoException(mensajeDelRechazo(e));
                }
                throw e;
            }
            if (reserva == null) {
                throw new IllegalStateException("Inventario no devolvió datos de la reserva");
            }
            compra.setReservaId(reserva.id()); // record no usa get
            // Inventario confirma el sector y la cantidad efectivamente retenidos.
            if (reserva.sectorId() != null) {
                compra.setSectorId(reserva.sectorId());
            }
            if (reserva.cantidad() != null) {
                compra.setCantidad(reserva.cantidad());
            }
            compra.addPaso("Lugares reservados (ID: " + reserva.id() + ")", "EJECUTADO");
            compraRepository.save(compra);

            // PASO 2: Cobrar en Pagos. El cliente resilient aplica timeouts,
            // reintentos (seguros por Idempotency-Key) y circuit breaker; su
            // fallback devuelve PENDIENTE_RECONCILIACION, nunca APROBADO.
            PagoResponse pago = pagosClientResiliente.cobrar(
                    compra.getId(), clienteId, request.precioTotal(), idempotencyKey);
            compra.setPagoId(pago.id()); // record no usa get

            if ("RECHAZADO".equals(pago.estado())) {
                inventarioClient.liberarReserva(compra.getReservaId());
                compra.addPaso("Pago rechazado - Liberando reserva (Compensación)", "COMPENSADO");
                compra.setEstado("PAGO_RECHAZADO");
                return compraRepository.save(compra);
            }

            if (!"APROBADO".equals(pago.estado())) {
                compra.addPaso("Pago " + pago.estado() + " - pendiente de reconciliación", "PENDIENTE");
                compra.setEstado("PAGO_INCIERTO");
                return compraRepository.save(compra);
            }

            compra.addPaso("Pago aprobado (ID: " + pago.id() + ")", "EJECUTADO");
            compraRepository.save(compra);
            return confirmarYEmitir(compra, reserva.importeTotal(), correlationId, email);

        } catch (LimiteEntradasExcedidoException e) {
            // La excepción es de negocio, no un fallo del sistema: se propaga el 422
            // y @Transactional revierte la compra PENDIENTE que se guardó recién.
            throw e;
        } catch (Exception e) {
            compra.setEstado("FALLO_SISTEMA");
            compra.addPaso("Error crítico: " + e.getMessage(), "FALLO");
            return compraRepository.save(compra);
        }
    }

    private Compra confirmarYEmitir(Compra compra, BigDecimal importeReserva, String correlationId, String email) {
        // PASO 3: Confirmar reserva definitiva
        try {
            inventarioClient.confirmarReserva(compra.getReservaId());
            compra.addPaso("Reserva confirmada definitivamente", "EJECUTADO");
            compraRepository.save(compra);
        } catch (FeignException e) {
            // COMPENSACIÓN 3: Si la reserva venció (pasaron 10 min), reintegrar el pago
            pagosClient.reintegrar(compra.getPagoId());
            compra.addPaso("Reserva vencida - Reintegro solicitado (Compensación)", "COMPENSADO");
            compra.setEstado("RESERVA_VENCIDA");
            return compraRepository.save(compra);
        }

        // PASO 4: Emitir Entradas
        if (compra.getSectorId() == null) {
            throw new IllegalStateException("Inventario no informó sectorId; no es posible registrar una venta auditable");
        }
        // Numerado: una entrada por asiento. Zona no numerada: una entrada por
        // unidad pedida, todas sin asientoId (el QR se genera igual por UUID).
        List<Long> asientos = compra.getAsientos() == null ? List.of() : compra.getAsientos();
        int totalEntradas = !asientos.isEmpty() ? asientos.size()
                : (compra.getCantidad() == null ? 0 : compra.getCantidad());
        if (totalEntradas <= 0) {
            throw new IllegalStateException("La compra no tiene asientos ni cantidad de entradas");
        }

        List<EntradaRequest> entradasAEmitir = new ArrayList<>();
        for (int i = 0; i < totalEntradas; i++) {
            Long asientoId = asientos.isEmpty() ? null : asientos.get(i);
            entradasAEmitir.add(new EntradaRequest(compra.getEventoId(), asientoId,
                    compra.getClienteId(), compra.getId(), compra.getSectorId()));
        }

        List<EntradaResponse> entradasEmitidas = entradaClient.emitirEntradas(entradasAEmitir).getBody();
        registrarLineasVenta(compra, entradasEmitidas, importeReserva);
        compra.addPaso("Entradas emitidas con éxito", "EJECUTADO");
        compra.setEstado("COMPLETADA");
        Compra compraCompletada = compraRepository.save(compra);
        compraCompletada.getLineasVenta().forEach(linea -> reporteVentaPublisher.publicar(linea, correlationId));
        compraCompletadaPublisher.publicar(compraCompletada, correlationId, email, entradasEmitidas);
        return compraCompletada;
    }

    @Scheduled(fixedDelayString = "${compras.reconciliacion.intervalo-ms:30000}")
    @Transactional
    public void reconciliarPagosInciertos() {
        for (Compra compra : compraRepository.findByEstado("PAGO_INCIERTO")) {
            try {
                List<PagoResponse> pagos = pagosClient.pagosDeCompra(compra.getId()).getBody();
                if (pagos == null || pagos.isEmpty()) {
                    continue;
                }
                Optional<PagoResponse> aprobado = pagos.stream()
                        .filter(p -> "APROBADO".equals(p.estado())).findFirst();
                Optional<PagoResponse> rechazado = pagos.stream()
                        .filter(p -> "RECHAZADO".equals(p.estado())).findFirst();

                if (aprobado.isPresent()) {
                    compra.setPagoId(aprobado.get().id());
                    compra.addPaso("Reconciliación: pago aprobado (ID: " + aprobado.get().id() + ")", "EJECUTADO");
                    compraRepository.save(compra);
                    confirmarYEmitir(compra, null, UUID.randomUUID().toString(), compra.getEmailCliente());
                } else if (rechazado.isPresent()) {
                    inventarioClient.liberarReserva(compra.getReservaId());
                    compra.addPaso("Reconciliación: pago rechazado - Liberando reserva", "COMPENSADO");
                    compra.setEstado("PAGO_RECHAZADO");
                    compraRepository.save(compra);
                }
            } catch (Exception e) {
                compra.addPaso("Reconciliación: " + e.getMessage(), "FALLO");
                compraRepository.save(compra);
            }
        }
    }

    private void registrarLineasVenta(Compra compra, List<EntradaResponse> entradas, BigDecimal importeReserva) {
        if (entradas == null || entradas.isEmpty()) {
            throw new IllegalStateException("No se emitieron entradas para una compra confirmada");
        }
        long totalCentavos = importeCentavos(importeReserva, compra.getPrecioTotal());
        long base = totalCentavos / entradas.size();
        long resto = totalCentavos % entradas.size();
        for (int i = 0; i < entradas.size(); i++) {
            EntradaResponse entrada = entradas.get(i);
            long importe = base + (i < resto ? 1 : 0);
            compra.getLineasVenta().add(new org.example.compraservice.entity.LineaVenta(
                    compra, entrada.id(), compra.getEventoId(), entrada.sectorId(), entrada.asientoId(), importe, "ARS"));
        }
    }

    private long importeCentavos(BigDecimal importeReserva, Double importeCompra) {
        if (importeReserva != null) {
            return importeReserva.movePointRight(2).longValueExact();
        }
        if (importeCompra == null) {
            throw new IllegalStateException("La compra no tiene importe confirmado");
        }
        return BigDecimal.valueOf(importeCompra).movePointRight(2).longValueExact();
    }

    /** Extrae el mensaje del 422 de inventario sin acoplarse al resto del formato del error. */
    private String mensajeDelRechazo(FeignException exception) {
        String contenido = exception.contentUTF8();
        if (contenido != null && !contenido.isBlank()) {
            try {
                JsonNode cuerpo = MAPPER.readTree(contenido);
                String mensaje = cuerpo.path("message").asText(null);
                if (mensaje != null && !mensaje.isBlank()) {
                    return mensaje;
                }
            } catch (Exception ignorado) {
                // El cuerpo no era JSON: se usa el mensaje genérico de abajo.
            }
        }
        return "El evento superó el límite de entradas por cliente";
    }

    public Compra obtenerPorId(Long id) {
        return compraRepository.findById(id).orElseThrow(() -> new RuntimeException("Compra no encontrada"));
    }

    public List<CompradorNotificacionResponse> compradoresCompletadosPorEvento(Long eventoId) {
        return compraRepository.findByEventoIdAndEstado(eventoId, "COMPLETADA").stream()
                .filter(compra -> compra.getEmailCliente() != null && !compra.getEmailCliente().isBlank())
                .collect(Collectors.toMap(
                        Compra::getEmailCliente,
                        compra -> new CompradorNotificacionResponse(compra.getClienteId(), compra.getEmailCliente()),
                        (primero, ignorado) -> primero
                )).values().stream().toList();
    }
}
