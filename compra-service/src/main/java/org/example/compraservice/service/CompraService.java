package org.example.compraservice.service;

import feign.FeignException;
import org.example.compraservice.client.*;
import org.example.compraservice.entity.Compra;
import org.example.compraservice.repository.CompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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

    private static final int MAX_ENTRADAS_POR_CLIENTE = 4; // Límite de negocio

    @Transactional
    public Compra procesarCompra(CompraRequest request, String idempotencyKey, Long clienteId) {

        // 1. Verificar Idempotencia
        Optional<Compra> compraExistente = compraRepository.findByIdempotencyKey(idempotencyKey);
        if (compraExistente.isPresent()) {
            return compraExistente.get();
        }

        // 2. Verificar límite por cliente
        long comprasPrevias = compraRepository.countByClienteIdAndEventoIdAndEstado(clienteId, request.getEventoId(), "COMPLETADA");
        if (comprasPrevias + request.getAsientos().size() > MAX_ENTRADAS_POR_CLIENTE) {
            throw new RuntimeException("Límite de entradas excedido para este evento");
        }

        // Inicializar la Compra
        Compra compra = new Compra();
        compra.setClienteId(clienteId);
        compra.setEventoId(request.getEventoId());
        compra.setIdempotencyKey(idempotencyKey);
        compra.setAsientos(request.getAsientos());
        compra.setPrecioTotal(request.getPrecioTotal());
        compra.setEstado("PENDIENTE");
        compra.addPaso("Inicio de compra", "EJECUTADO");
        compraRepository.save(compra);

        try {
            // PASO 1: Reservar en Inventario (10 minutos)
            ReservaResponse reserva = inventarioClient.reservarAsientos(
                new ReservaRequest(request.getEventoId(), request.getAsientos())
            ).getBody();
            compra.setReservaId(reserva.getId());
            compra.addPaso("Asientos reservados (ID: " + reserva.getId() + ")", "EJECUTADO");
            compraRepository.save(compra);

            // PASO 2: Cobrar en Pagos
            try {
                PagoResponse pago = pagosClient.cobrar(
                    new PagoRequest(clienteId, request.getPrecioTotal(), idempotencyKey)
                ).getBody();
                compra.setPagoId(pago.getId());
                compra.addPaso("Pago aprobado (ID: " + pago.getId() + ")", "EJECUTADO");
                compraRepository.save(compra);
            } catch (FeignException e) {
                // COMPENSACIÓN 2: Si falla el pago, liberar la reserva
                inventarioClient.liberarReserva(compra.getReservaId());
                compra.addPaso("Pago rechazado - Liberando reserva (Compensación)", "COMPENSADO");
                compra.setEstado("PAGO_RECHAZADO");
                return compraRepository.save(compra);
            }

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
            List<EntradaRequest> entradasAEmitir = request.getAsientos().stream()
                .map(asientoId -> new EntradaRequest(request.getEventoId(), asientoId, clienteId, compra.getId()))
                .collect(Collectors.toList());

            entradaClient.emitirEntradas(entradasAEmitir);
            compra.addPaso("Entradas emitidas con éxito", "EJECUTADO");
            compra.setEstado("COMPLETADA");

            return compraRepository.save(compra);

        } catch (Exception e) {
            compra.setEstado("FALLO_SISTEMA");
            compra.addPaso("Error crítico: " + e.getMessage(), "FALLO");
            return compraRepository.save(compra);
        }
    }

    public Compra obtenerPorId(Long id) {
        return compraRepository.findById(id).orElseThrow(() -> new RuntimeException("Compra no encontrada"));
    }
}
