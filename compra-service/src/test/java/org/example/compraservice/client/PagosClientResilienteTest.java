package org.example.compraservice.client;

import feign.FeignException;
import org.example.compraservice.dto.PagoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class PagosClientResilienteTest {

    @Test
    @DisplayName("El fallback ante timeout no aprueba: deja el pago pendiente de reconciliación")
    void fallbackNuncaAprueba() {
        PagosClientResiliente cliente = new PagosClientResiliente(null);
        FeignException timeout = mock(FeignException.class);

        PagoResponse resultado = cliente.cobrarFallback(1L, 5000.0, "key-timeout", timeout);

        assertNull(resultado.id());
        assertNotEquals("APROBADO", resultado.estado());
        assertEquals(PagosClientResiliente.ESTADO_FALLBACK, resultado.estado());
    }
}
