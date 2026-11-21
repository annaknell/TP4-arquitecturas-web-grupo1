package org.example.compraservice.service;

import feign.FeignException;
import org.example.compraservice.client.*;
import org.example.compraservice.dto.*;
import org.example.compraservice.entity.Compra;
import org.example.compraservice.messaging.CompraCompletadaPublisher;
import org.example.compraservice.messaging.ReporteVentaPublisher;
import org.example.compraservice.repository.CompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class CompraService {

    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private InventarioClient inventarioClient;
    @Autowired
    private PagosClient pagosClient;
    @Autowired
    private EntradaClient entradaClient;
    @Autowired
    private CompraCompletadaPublisher compraCompletadaPublisher;
    @Autowired
    private ReporteVentaPublisher reporteVentaPublisher;

    private static final int MAX_ENTRADAS_POR_CLIENTE = 4; // Límite de negocio

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

        // 2. Verificar límite por cliente
        long comprasPrevias = compraRepository.countByClienteIdAndEventoIdAndEstado(clienteId, request.eventoId(), "COMPLETADA");
        if (comprasPrevias + request.asientos().size() > MAX_ENTRADAS_POR_CLIENTE) {
            throw new RuntimeException("Límite de entradas excedido para este evento");
        }

        // Inicializar la Compra
        Compra compra = new Compra();
        compra.setClienteId(clienteId);
        compra.setEmailCliente(email);
        compra.setEventoId(request.eventoId());
        compra.setIdempotencyKey(idempotencyKey);
        compra.setAsientos(request.asientos());
        compra.setPrecioTotal(request.precioTotal());
        compra.setEstado("PENDIENTE");
        compra.addPaso("Inicio de compra", "EJECUTADO");
        compraRepository.save(compra);

        try {
            // PASO 1: Reservar en Inventario (10 minutos)
            ReservaResponse reserva = inventarioClient.reservarAsientos(
                new ReservaRequest(request.eventoId(), clienteId, request.asientos(), request.autorizacionFila(), idempotencyKey)
            ).getBody();
            compra.setReservaId(reserva.id()); // record no usa get
            compra.setSectorId(reserva.sectorId());
            compra.addPaso("Asientos reservados (ID: " + reserva.id() + ")", "EJECUTADO");
            compraRepository.save(compra);

            // PASO 2: Cobrar en Pagos
            PagoResponse pago;
            try {
                pago = pagosClient.cobrar(
                    new PagoRequest(clienteId, request.precioTotal(), idempotencyKey)
                ).getBody();
            } catch (FeignException e) {
                // COMPENSACIÓN 2: Si falla el pago, liberar la reserva
                inventarioClient.liberarReserva(compra.getReservaId());
                compra.addPaso("Pago rechazado - Liberando reserva (Compensación)", "COMPENSADO");
                compra.setEstado("PAGO_RECHAZADO");
                return compraRepository.save(compra);
            }
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
        List<EntradaRequest> entradasAEmitir = compra.getAsientos().stream()
                .map(asientoId -> new EntradaRequest(compra.getEventoId(), asientoId, compra.getClienteId(),
                        compra.getId(), compra.getSectorId()))
                .collect(Collectors.toList());

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

    public Compra obtenerPorId(Long id) {
        return compraRepository.findById(id).orElseThrow(() -> new RuntimeException("Compra no encontrada"));
    }

    public List<CompradorNotificacionResponse> compradoresCompletadosPorEvento(Long eventoId) {
        return compraRepository.findByEventoIdAndEstado(eventoId, "COMPLETADA").stream()
                .filter(compra -> compra.getEmailCliente() != null && !compra.getEmailCliente().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        Compra::getEmailCliente,
                        compra -> new CompradorNotificacionResponse(compra.getClienteId(), compra.getEmailCliente()),
                        (primero, ignorado) -> primero
                )).values().stream().toList();
    }
}
