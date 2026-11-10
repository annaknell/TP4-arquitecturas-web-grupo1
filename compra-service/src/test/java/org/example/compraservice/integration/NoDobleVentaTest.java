package org.example.compraservice.integration;

import org.example.compraservice.client.EntradaClient;
import org.example.compraservice.client.InventarioClient;
import org.example.compraservice.client.PagosClient;
import org.example.compraservice.dto.CompraRequest;
import org.example.compraservice.dto.PagoRequest;
import org.example.compraservice.dto.PagoResponse;
import org.example.compraservice.dto.ReservaRequest;
import org.example.compraservice.dto.ReservaResponse;
import org.example.compraservice.entity.Compra;
import org.example.compraservice.repository.CompraRepository;
import org.example.compraservice.service.CompraService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoDobleVentaTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private PagosClient pagosClient;

    @Mock
    private EntradaClient entradaClient;

    @InjectMocks
    private CompraService compraService;

    @Test
    @DisplayName("Concurrencia: 10 peticiones en simultáneo sobre el mismo asiento solo permite 1 reserva exitosa")
    void verificarNoDobleVentaSimultanea() throws InterruptedException {
        int cantidadUsuariosSimultaneos = 10;
        Long eventoId = 15L;
        Long asientoConflictivo = 101L;

        ExecutorService executor = Executors.newFixedThreadPool(cantidadUsuariosSimultaneos);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch ejecucionCompleta = new CountDownLatch(cantidadUsuariosSimultaneos);

        AtomicInteger comprasExitosas = new AtomicInteger(0);
        AtomicInteger comprasFallidas = new AtomicInteger(0);

        // 1. Inventario: El primer hilo aprueba la reserva, los siguientes 9 rebotan por conflicto
        AtomicInteger llamadasInventario = new AtomicInteger(0);
        when(inventarioClient.reservarAsientos(any(ReservaRequest.class))).thenAnswer(invocation -> {
            if (llamadasInventario.getAndIncrement() == 0) {
                return ResponseEntity.ok(new ReservaResponse(99L, "RESERVADO"));
            } else {
                throw new RuntimeException("El asiento 101 ya se encuentra reservado por otro usuario");
            }
        });

        // 2. Pagos: Si la reserva pasó, el cobro se aprueba exitosamente
        when(pagosClient.cobrar(any(PagoRequest.class)))
            .thenReturn(ResponseEntity.ok(new PagoResponse(555L, "APROBADO")));

        // 3. Mocks de base de datos y repositorios
        when(compraRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(compraRepository.countByClienteIdAndEventoIdAndEstado(anyLong(), eq(eventoId), eq("COMPLETADA"))).thenReturn(0L);
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        // Lanzamos 10 hilos concurrentes
        for (long usuarioId = 1; usuarioId <= cantidadUsuariosSimultaneos; usuarioId++) {
            final long idFinal = usuarioId;
            executor.execute(() -> {
                try {
                    latch.await(); // Disparo sincronizado
                    CompraRequest request = new CompraRequest(eventoId, List.of(asientoConflictivo), 5000.0);
                    Compra resultado = compraService.procesarCompra(request, "idemp-conc-" + idFinal, idFinal);

                    if ("COMPLETADA".equals(resultado.getEstado())) {
                        comprasExitosas.incrementAndGet();
                    } else {
                        comprasFallidas.incrementAndGet();
                    }
                } catch (Exception e) {
                    comprasFallidas.incrementAndGet();
                } finally {
                    ejecucionCompleta.countDown();
                }
            });
        }

        latch.countDown(); // Señal de inicio
        ejecucionCompleta.await();
        executor.shutdown();

        // Verificaciones
        assertEquals(1, comprasExitosas.get(), "Solo 1 usuario debe lograr completar la compra");
        assertEquals(9, comprasFallidas.get(), "Los otros 9 usuarios deben rebotar por falta de disponibilidad");
    }
}