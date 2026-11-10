package org.example.entradaservice;

import org.example.entradaservice.entity.Entrada;
import org.example.entradaservice.repository.EntradaRepository;
import org.example.entradaservice.service.EntradaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntradaServiceTest {

    @Mock
    private EntradaRepository entradaRepository;

    @InjectMocks
    private EntradaService entradaService;

    @Test
    @DisplayName("Asigna estado EMITIDA y genera código QR si viene nulo")
    void emitirEntradaGeneraQRyEstado() {
        Entrada entrada = new Entrada();
        entrada.setClienteId(1L);
        entrada.setEventoId(15L);

        when(entradaRepository.save(any(Entrada.class))).thenAnswer(i -> i.getArgument(0));

        Entrada resultado = entradaService.emitirEntrada(entrada);

        assertNotNull(resultado);
        assertEquals("EMITIDA", resultado.getEstado());
        assertNotNull(resultado.getCodigoQr());
        assertTrue(resultado.getCodigoQr().startsWith("QR-"));
        verify(entradaRepository, times(1)).save(entrada);
    }

    @Test
    @DisplayName("Cancelar entradas por evento cambia EMITIDA a CANCELADA y retorna lista de compras únicas")
    void cancelarEntradasPorEvento() {
        Long eventoId = 15L;

        Entrada e1 = new Entrada();
        e1.setEstado("EMITIDA");
        e1.setCompraId(200L);

        Entrada e2 = new Entrada();
        e2.setEstado("EMITIDA");
        e2.setCompraId(200L); // Compra repetida para validar distinct()

        when(entradaRepository.findByEventoIdAndEstadoIn(eq(eventoId), anyList()))
            .thenReturn(List.of(e1, e2));

        List<Long> comprasAfectadas = entradaService.cancelarEntradasPorEvento(eventoId);

        assertEquals(1, comprasAfectadas.size());
        assertEquals(200L, comprasAfectadas.get(0));
        assertEquals("CANCELADA", e1.getEstado());
        assertEquals("CANCELADA", e2.getEstado());
        verify(entradaRepository, times(1)).saveAll(any());
    }
}