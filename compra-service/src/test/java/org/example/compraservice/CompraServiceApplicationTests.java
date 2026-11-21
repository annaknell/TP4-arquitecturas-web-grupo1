package org.example.compraservice;

import org.example.compraservice.client.EntradaClient;
import org.example.compraservice.client.CatalogoClient;
import org.example.compraservice.client.InventarioClient;
import org.example.compraservice.client.PagosClient;
import org.example.compraservice.dto.CompraRequest;
import org.example.compraservice.dto.ReservaRequest;
import org.example.compraservice.dto.ReservaResponse;
import org.example.compraservice.dto.EntradaResponse;
import org.example.compraservice.dto.EventoNotificacionResponse;
import org.example.compraservice.entity.Compra;
import org.example.compraservice.repository.CompraRepository;
import org.example.compraservice.messaging.CompraCompletadaPublisher;
import org.example.compraservice.service.CompraService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private PagosClient pagosClient;

    @Mock
    private EntradaClient entradaClient;

    @Mock
    private CatalogoClient catalogoClient;

    @Mock
    private CompraCompletadaPublisher compraCompletadaPublisher;

    @InjectMocks
    private CompraService compraService;

    @Test
    @DisplayName("Lanza RuntimeException si la suma de compras excede el límite de 4 entradas")
    void rechazarSuperacionDeLimiteEntradas() {
        Long clienteId = 1L;
        String idempotencyKey = "key-limite-1";
        CompraRequest request = new CompraRequest(15L, List.of(101L, 102L, 103L, 104L, 105L), 25000.0);

        when(compraRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(compraRepository.countByClienteIdAndEventoIdAndEstado(clienteId, 15L, "COMPLETADA")).thenReturn(0L);

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> compraService.procesarCompra(request, idempotencyKey, clienteId)
        );

        assertEquals("Límite de entradas excedido para este evento", exception.getMessage());
        verifyNoInteractions(inventarioClient);
        verify(compraRepository, never()).save(any());
    }

    @Test
    @DisplayName("Captura excepciones del inventario y marca la compra en FALLO_SISTEMA")
    void registrarFalloAnteExcepcionDeInventario() {
        Long clienteId = 1L;
        String idempotencyKey = "key-fallo-1";
        CompraRequest request = new CompraRequest(15L, List.of(101L), 5000.0);

        when(compraRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(compraRepository.countByClienteIdAndEventoIdAndEstado(clienteId, 15L, "COMPLETADA")).thenReturn(0L);
        when(inventarioClient.reservarAsientos(any(ReservaRequest.class)))
            .thenThrow(new RuntimeException("Conexión rehusada en Inventario"));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        Compra compraResultado = compraService.procesarCompra(request, idempotencyKey, clienteId);

        assertNotNull(compraResultado);
        assertEquals("FALLO_SISTEMA", compraResultado.getEstado());
        verify(compraRepository, atLeastOnce()).save(any(Compra.class));
    }

    @Test
    @DisplayName("Completa la compra y publica CompraCompletada después de emitir las entradas")
    void completarCompraYPublicarEvento() {
        Long clienteId = 1L;
        CompraRequest request = new CompraRequest(15L, List.of(101L), 5000.0);

        when(compraRepository.findByIdempotencyKey("key-ok")).thenReturn(Optional.empty());
        when(compraRepository.countByClienteIdAndEventoIdAndEstado(clienteId, 15L, "COMPLETADA")).thenReturn(0L);
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> {
            Compra compra = invocation.getArgument(0);
            if (compra.getId() == null) {
                compra.setId(123L);
            }
            return compra;
        });
        when(inventarioClient.reservarAsientos(any(ReservaRequest.class)))
                .thenReturn(ResponseEntity.ok(new ReservaResponse(10L, "RESERVADO")));
        when(pagosClient.cobrar(any()))
                .thenReturn(ResponseEntity.ok(new org.example.compraservice.dto.PagoResponse(20L, "APROBADO")));
        when(entradaClient.emitirEntradas(any()))
                .thenReturn(ResponseEntity.ok(List.of(new EntradaResponse(800L, "QR-800", "EMITIDA", 15L, 1L, 101L, clienteId, 123L))));

        Compra resultado = compraService.procesarCompra(request, "key-ok", clienteId, "corr-123", "cliente@example.com");

        assertEquals("COMPLETADA", resultado.getEstado());
        verify(entradaClient).emitirEntradas(any());
        verify(compraCompletadaPublisher).publicar(eq(resultado), eq("corr-123"),
                eq("cliente@example.com"), any());
    }

    @Test
    @DisplayName("Un error al publicar no se propaga ni invalida la compra")
    void ignorarFalloDePublicacion() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        CatalogoClient catalogo = mock(CatalogoClient.class);
        CompraCompletadaPublisher publisher = new CompraCompletadaPublisher(rabbitTemplate, catalogo);
        Compra compra = new Compra();
        compra.setId(123L);
        compra.setClienteId(1L);
        compra.setEventoId(15L);
        compra.setAsientos(List.of(101L));
        when(catalogo.obtenerParaNotificacion(15L)).thenReturn(new EventoNotificacionResponse(
                15L, "Recital", java.time.LocalDateTime.of(2026, 12, 20, 21, 0),
                "Teatro X", java.util.Map.of(1L, "Plateas")
        ));

        doThrow(new RuntimeException("RabbitMQ no disponible"))
                .when(rabbitTemplate).convertAndSend(
                        anyString(), anyString(),
                        any(org.example.compraservice.dto.CompraCompletadaEvent.class));

        assertDoesNotThrow(() -> publisher.publicar(compra, "corr-123", "cliente@example.com",
                List.of(new EntradaResponse(800L, "QR-800", "EMITIDA", 15L, 1L, 101L, 1L, 123L))));
    }
}
