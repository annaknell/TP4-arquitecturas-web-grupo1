package org.example.compraservice;

import org.example.compraservice.client.EntradaClient;
import org.example.compraservice.client.InventarioClient;
import org.example.compraservice.client.PagosClient;
import org.example.compraservice.dto.CompraRequest;
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
}