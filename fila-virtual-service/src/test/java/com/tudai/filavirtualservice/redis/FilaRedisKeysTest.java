package com.tudai.filavirtualservice.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FilaRedisKeysTest {

    @Test
    void construyeLasClavesDelMismoEventoBajoElMismoPrefijo() {
        assertEquals("fila-virtual:evento:25:cola", FilaRedisKeys.cola(25L));
        assertEquals("fila-virtual:evento:25:configuracion", FilaRedisKeys.configuracion(25L));
        assertEquals("fila-virtual:evento:25:ticket:7", FilaRedisKeys.ticket(25L, 7L));
    }

    @Test
    void rechazaIdentificadoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> FilaRedisKeys.cola(0L));
        assertThrows(IllegalArgumentException.class, () -> FilaRedisKeys.ticket(1L, null));
    }
}
