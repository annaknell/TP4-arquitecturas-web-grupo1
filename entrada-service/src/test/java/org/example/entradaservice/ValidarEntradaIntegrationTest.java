package org.example.entradaservice;

import org.example.entradaservice.controller.EntradaController;
import org.example.entradaservice.dto.ValidarEntradaRequest;
import org.example.entradaservice.dto.ValidarEntradaResponse;
import org.example.entradaservice.entity.Entrada;
import org.example.entradaservice.repository.EntradaRepository;
import org.example.entradaservice.service.EntradaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ValidarEntradaIntegrationTest {

    @Autowired
    private EntradaService entradaService;

    @Autowired
    private EntradaController entradaController;

    @Autowired
    private EntradaRepository entradaRepository;

    @BeforeEach
    void limpiar() {
        entradaRepository.deleteAll();
    }

    private Entrada crearEntrada(String codigoQr, Long eventoId) {
        Entrada entrada = new Entrada();
        entrada.setCodigoQr(codigoQr);
        entrada.setEstado("EMITIDA");
        entrada.setEventoId(eventoId);
        entrada.setSectorId(1L);
        entrada.setAsientoId(5L);
        entrada.setClienteId(1L);
        entrada.setCompraId(1L);
        return entradaRepository.save(entrada);
    }

    @Test
    @DisplayName("Dos puertas escaneando el mismo QR a la vez: solo una autoriza")
    void dosLecturasSimultaneasSoloAutorizanUna() throws Exception {
        crearEntrada("QR-CONCURRENTE", 900L);

        int puertas = 2;
        ExecutorService pool = Executors.newFixedThreadPool(puertas);
        CyclicBarrier barrera = new CyclicBarrier(puertas);
        List<Future<ValidarEntradaResponse>> lecturas = new ArrayList<>();
        for (int i = 0; i < puertas; i++) {
            lecturas.add(pool.submit(() -> {
                barrera.await();
                return entradaService.validar("QR-CONCURRENTE", 900L);
            }));
        }

        long autorizadas = 0;
        for (Future<ValidarEntradaResponse> lectura : lecturas) {
            if (lectura.get().isAutorizado()) {
                autorizadas++;
            }
        }
        pool.shutdown();

        assertEquals(1, autorizadas);
        Entrada guardada = entradaRepository.findByCodigoQr("QR-CONCURRENTE").orElseThrow();
        assertEquals("USADA", guardada.getEstado());
        assertNotNull(guardada.getUsadaEn());
    }

    @Test
    @DisplayName("El endpoint responde 200 al autorizar, 404 si no existe y 409 con el motivo")
    void respuestasHttpDelEndpoint() {
        crearEntrada("QR-PUERTA-1", 900L);

        ResponseEntity<ValidarEntradaResponse> autorizada =
                entradaController.validarEntrada(new ValidarEntradaRequest("QR-PUERTA-1", 900L));
        assertEquals(200, autorizada.getStatusCode().value());
        assertTrue(autorizada.getBody().isAutorizado());
        assertEquals(1L, autorizada.getBody().getSectorId());
        assertEquals(5L, autorizada.getBody().getAsientoId());

        ResponseEntity<ValidarEntradaResponse> repetida =
                entradaController.validarEntrada(new ValidarEntradaRequest("QR-PUERTA-1", 900L));
        assertEquals(409, repetida.getStatusCode().value());
        assertEquals("YA_USADA", repetida.getBody().getMotivo());
        assertNotNull(repetida.getBody().getUsadaEn());

        ResponseEntity<ValidarEntradaResponse> inexistente =
                entradaController.validarEntrada(new ValidarEntradaRequest("QR-NO-EXISTE", 900L));
        assertEquals(404, inexistente.getStatusCode().value());
        assertEquals("INEXISTENTE", inexistente.getBody().getMotivo());

        crearEntrada("QR-PUERTA-2", 900L);
        ResponseEntity<ValidarEntradaResponse> otroEvento =
                entradaController.validarEntrada(new ValidarEntradaRequest("QR-PUERTA-2", 901L));
        assertEquals(409, otroEvento.getStatusCode().value());
        assertEquals("EVENTO_EQUIVOCADO", otroEvento.getBody().getMotivo());
    }
}
