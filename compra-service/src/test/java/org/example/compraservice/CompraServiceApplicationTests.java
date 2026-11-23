package org.example.compraservice;

import feign.FeignException;
import org.example.compraservice.client.EntradaClient;
import org.example.compraservice.client.CatalogoClient;
import org.example.compraservice.client.InventarioClient;
import org.example.compraservice.client.PagosClientResiliente;
import org.example.compraservice.dto.CompraRequest;
import org.example.compraservice.dto.EntradaRequest;
import org.example.compraservice.dto.ReservaRequest;
import org.example.compraservice.dto.ReservaResponse;
import org.example.compraservice.dto.EntradaResponse;
import org.example.compraservice.dto.EventoNotificacionResponse;
import org.example.compraservice.entity.Compra;
import org.example.compraservice.exception.LimiteEntradasExcedidoException;
import org.example.compraservice.repository.CompraRepository;
import org.example.compraservice.messaging.CompraCompletadaPublisher;
import org.example.compraservice.messaging.ReporteVentaPublisher;
import org.example.compraservice.service.CompraService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private PagosClientResiliente pagosClientResiliente;

    @Mock
    private EntradaClient entradaClient;

    @Mock
    private CatalogoClient catalogoClient;

    @Mock
    private CompraCompletadaPublisher compraCompletadaPublisher;

    @Mock
    private ReporteVentaPublisher reporteVentaPublisher;

    @InjectMocks
    private CompraService compraService;

    @Test
    @DisplayName("Propaga el 422 de inventario con el detalle del límite (US-18)")
    void propagarRechazoPorLimiteDeInventario() {
        Long clienteId = 1L;
        String idempotencyKey = "key-limite-1";
        CompraRequest request = new CompraRequest(15L, null,
                List.of(101L, 102L, 103L, 104L, 105L), null, 25000.0, null);
        String detalle = "El evento permite 6 entradas por cliente y ya tiene 5";

        FeignException rechazo = mock(FeignException.class);
        when(rechazo.status()).thenReturn(422);
        when(rechazo.contentUTF8()).thenReturn("{\"message\":\"" + detalle + "\"}");

        when(compraRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(inventarioClient.reservarAsientos(any(ReservaRequest.class))).thenThrow(rechazo);
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        LimiteEntradasExcedidoException exception = assertThrows(
            LimiteEntradasExcedidoException.class,
            () -> compraService.procesarCompra(request, idempotencyKey, clienteId)
        );

        assertEquals(detalle, exception.getMessage());
        verify(pagosClientResiliente, never()).cobrar(any(), any(), any(), any());
        verify(entradaClient, never()).emitirEntradas(any());
    }

    @Test
    @DisplayName("Captura excepciones del inventario y marca la compra en FALLO_SISTEMA")
    void registrarFalloAnteExcepcionDeInventario() {
        Long clienteId = 1L;
        String idempotencyKey = "key-fallo-1";
        CompraRequest request = new CompraRequest(15L, null, List.of(101L), null, 5000.0, null);

        when(compraRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
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
        CompraRequest request = new CompraRequest(15L, null, List.of(101L), null, 5000.0, null);

        when(compraRepository.findByIdempotencyKey("key-ok")).thenReturn(Optional.empty());
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> {
            Compra compra = invocation.getArgument(0);
            if (compra.getId() == null) {
                compra.setId(123L);
            }
            return compra;
        });
        when(inventarioClient.reservarAsientos(any(ReservaRequest.class)))
                .thenReturn(ResponseEntity.ok(new ReservaResponse(10L, "RESERVADO", 15L, 1L, 1,
                        new java.math.BigDecimal("5000"), new java.math.BigDecimal("5000"))));
        when(pagosClientResiliente.cobrar(any(), any(), any(), any()))
                .thenReturn(new org.example.compraservice.dto.PagoResponse(20L, "APROBADO"));
        when(entradaClient.emitirEntradas(any()))
                .thenReturn(ResponseEntity.ok(List.of(new EntradaResponse(800L, "QR-800", "EMITIDA", 15L, 1L, 101L, clienteId, 123L))));

        Compra resultado = compraService.procesarCompra(request, "key-ok", clienteId, "corr-123", "cliente@example.com");

        assertEquals("COMPLETADA", resultado.getEstado());
        verify(entradaClient).emitirEntradas(any());
        verify(compraCompletadaPublisher).publicar(eq(resultado), eq("corr-123"),
                eq("cliente@example.com"), any());
    }

    @Test
    @DisplayName("Fallback del cobro: la compra queda PAGO_INCIERTO, nunca se aprueba ni emite entradas")
    void fallbackDePagosDejaCompraIncierta() {
        Long clienteId = 1L;
        CompraRequest request = new CompraRequest(15L, null, List.of(101L), null, 5000.0, null);

        when(compraRepository.findByIdempotencyKey("key-incierto")).thenReturn(Optional.empty());
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));
        when(inventarioClient.reservarAsientos(any(ReservaRequest.class)))
                .thenReturn(ResponseEntity.ok(new ReservaResponse(10L, "RESERVADO", 15L, 1L, 1,
                        new java.math.BigDecimal("5000"), new java.math.BigDecimal("5000"))));
        when(pagosClientResiliente.cobrar(any(), any(), any(), any()))
                .thenReturn(new org.example.compraservice.dto.PagoResponse(
                        null, PagosClientResiliente.ESTADO_FALLBACK));

        Compra resultado = compraService.procesarCompra(request, "key-incierto", clienteId);

        assertEquals("PAGO_INCIERTO", resultado.getEstado());
        verify(entradaClient, never()).emitirEntradas(any());
    }

    @Test
    @DisplayName("Zona no numerada: propaga sectorId/cantidad y emite N entradas sin asientoId")
    @SuppressWarnings("unchecked")
    void comprarPorCantidadEnZonaNoNumerada() {
        Long clienteId = 1L;
        CompraRequest request = new CompraRequest(15L, 7L, null, 3, 15000.0, null);

        when(compraRepository.findByIdempotencyKey("key-zona")).thenReturn(Optional.empty());
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> {
            Compra compra = invocation.getArgument(0);
            if (compra.getId() == null) {
                compra.setId(123L);
            }
            return compra;
        });
        when(inventarioClient.reservarAsientos(any(ReservaRequest.class)))
                .thenReturn(ResponseEntity.ok(new ReservaResponse(11L, "RESERVADO", 15L, 7L, 3,
                        new java.math.BigDecimal("5000"), new java.math.BigDecimal("15000"))));
        when(pagosClientResiliente.cobrar(any(), any(), any(), any()))
                .thenReturn(new org.example.compraservice.dto.PagoResponse(21L, "APROBADO"));
        when(entradaClient.emitirEntradas(any()))
                .thenReturn(ResponseEntity.ok(List.of(
                        new EntradaResponse(801L, "QR-801", "EMITIDA", 15L, 7L, null, clienteId, 123L),
                        new EntradaResponse(802L, "QR-802", "EMITIDA", 15L, 7L, null, clienteId, 123L),
                        new EntradaResponse(803L, "QR-803", "EMITIDA", 15L, 7L, null, clienteId, 123L))));

        Compra resultado = compraService.procesarCompra(request, "key-zona", clienteId);

        assertEquals("COMPLETADA", resultado.getEstado());
        assertEquals(7L, resultado.getSectorId());
        assertEquals(3, resultado.getCantidad());
        assertTrue(resultado.getAsientos().isEmpty());

        ArgumentCaptor<ReservaRequest> reservaCaptor = ArgumentCaptor.forClass(ReservaRequest.class);
        verify(inventarioClient).reservarAsientos(reservaCaptor.capture());
        assertEquals(15L, reservaCaptor.getValue().eventoId());
        assertEquals(7L, reservaCaptor.getValue().sectorId());
        assertEquals(3, reservaCaptor.getValue().cantidad());
        assertNull(reservaCaptor.getValue().asientos());

        ArgumentCaptor<List<EntradaRequest>> entradasCaptor = ArgumentCaptor.forClass(List.class);
        verify(entradaClient).emitirEntradas(entradasCaptor.capture());
        List<EntradaRequest> emitidas = entradasCaptor.getValue();
        assertEquals(3, emitidas.size());
        assertTrue(emitidas.stream().allMatch(entrada -> entrada.asientoId() == null));
        assertTrue(emitidas.stream().allMatch(entrada -> entrada.sectorId().equals(7L)));

        assertEquals(3, resultado.getLineasVenta().size());
        assertTrue(resultado.getLineasVenta().stream().allMatch(linea -> linea.getAsientoId() == null));
        assertEquals(500000L, resultado.getLineasVenta().get(0).getImporteCentavos());
    }

    @Test
    @DisplayName("Rechaza selecciones inválidas antes de reservar")
    void validarSeleccionExcluyente() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompraRequest(15L, 7L, List.of(101L), 1, 5000.0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CompraRequest(15L, null, null, null, 5000.0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CompraRequest(15L, null, null, 3, 5000.0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CompraRequest(15L, null, List.of(101L), 0, 5000.0, null));
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
