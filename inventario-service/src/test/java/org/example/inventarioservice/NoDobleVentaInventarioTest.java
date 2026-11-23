package org.example.inventarioservice;

import org.example.inventarioservice.client.FilaVirtualClient;
import org.example.inventarioservice.dto.ProvisionDTO;
import org.example.inventarioservice.dto.ReservaRequestDTO;
import org.example.inventarioservice.entity.ConfiguracionEvento;
import org.example.inventarioservice.entity.EventoAsiento;
import org.example.inventarioservice.entity.TipoSector;
import org.example.inventarioservice.entity.ZonaCupo;
import org.example.inventarioservice.exception.ConflictoException;
import org.example.inventarioservice.repository.EventoAsientoRepository;
import org.example.inventarioservice.repository.ZonaCupoRepository;
import org.example.inventarioservice.service.InventarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

//prueba de no doble venta con base de datos real (US-23, EN-04)

@SpringBootTest(properties = {
        // base de datos H2 en memoria, solo para esta prueba
        "spring.datasource.url=jdbc:h2:mem:inventario_no_doble_venta;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NoDobleVentaInventarioTest {

    private static final int CLIENTES_SIMULTANEOS = 50;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private EventoAsientoRepository asientoRepository;

    @Autowired
    private ZonaCupoRepository zonaRepository;

    @MockitoBean
    private FilaVirtualClient filaVirtualClient;

    @BeforeEach
    void sinFilaVirtual() {
        when(filaVirtualClient.validarYConsumir(any(), any(), any()))
                .thenReturn(new FilaVirtualClient.ResultadoAutorizacion(true, "NO_REQUIERE_FILA"));
    }

    @Test
    @DisplayName("50 clientes a la vez sobre el mismo asiento numerado: solo 1 lo reserva (US-23)")
    void mismoAsientoSoloUnaReserva() throws InterruptedException {
        Long eventoId = 100L;
        Long sectorId = 1L;
        Long asientoId = 101L;
        inventarioService.provisionar(new ProvisionDTO(eventoId, ConfiguracionEvento.Estado.PUBLICADO, 6, List.of(
                new ProvisionDTO.SectorDef(sectorId, TipoSector.NUMERADO, null,
                        List.of(new ProvisionDTO.AsientoDef(asientoId, "A", "1"))))));

        Resultados resultados = lanzarEnSimultaneo(clienteId -> inventarioService.reservar(
                new ReservaRequestDTO(clienteId, eventoId, sectorId, List.of(asientoId), null,
                        BigDecimal.valueOf(5000), "asiento-cliente-" + clienteId, null)));

        assertThat(resultados.exitosas.get()).as("reservas exitosas").isEqualTo(1);
        assertThat(resultados.rechazadas.get()).as("reservas rechazadas").isEqualTo(CLIENTES_SIMULTANEOS - 1);

        EventoAsiento asiento = asientoRepository.findByEventIdAndAsientoIdIn(eventoId, List.of(asientoId)).get(0);
        assertThat(asiento.getEstado()).isEqualTo(EventoAsiento.Estado.RETENIDO);
        assertThat(asiento.getReservaId()).isNotNull();
    }

    @Test
    @DisplayName("50 clientes a la vez por el último lugar de una zona: solo 1 lo reserva (US-23)")
    void ultimoLugarDeZonaSoloUnaReserva() throws InterruptedException {
        Long eventoId = 200L;
        Long sectorId = 2L;
        inventarioService.provisionar(new ProvisionDTO(eventoId, ConfiguracionEvento.Estado.PUBLICADO, 6, List.of(
                new ProvisionDTO.SectorDef(sectorId, TipoSector.GENERAL, 1, null))));

        Resultados resultados = lanzarEnSimultaneo(clienteId -> inventarioService.reservar(
                new ReservaRequestDTO(clienteId, eventoId, sectorId, null, 1,
                        BigDecimal.valueOf(3000), "zona-cliente-" + clienteId, null)));

        assertThat(resultados.exitosas.get()).as("reservas exitosas").isEqualTo(1);
        assertThat(resultados.rechazadas.get()).as("reservas rechazadas").isEqualTo(CLIENTES_SIMULTANEOS - 1);

        ZonaCupo zona = zonaRepository.findByEventId(eventoId).get(0);
        assertThat(zona.getRetenidos() + zona.getVendidos()).isEqualTo(1);
    }


    private interface Intento {
        void ejecutar(Long clienteId) throws Exception;
    }

    private static class Resultados {
        final AtomicInteger exitosas = new AtomicInteger();
        final AtomicInteger rechazadas = new AtomicInteger();
    }

    private Resultados lanzarEnSimultaneo(Intento intento) throws InterruptedException {
        Resultados resultados = new Resultados();
        ExecutorService hilos = Executors.newFixedThreadPool(CLIENTES_SIMULTANEOS);
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch terminaron = new CountDownLatch(CLIENTES_SIMULTANEOS);

        for (long clienteId = 1; clienteId <= CLIENTES_SIMULTANEOS; clienteId++) {
            final long id = clienteId;
            hilos.execute(() -> {
                try {
                    largada.await();
                    intento.ejecutar(id);
                    resultados.exitosas.incrementAndGet();
                } catch (ConflictoException e) {
                    resultados.rechazadas.incrementAndGet();
                } catch (Exception e) {
                    resultados.rechazadas.incrementAndGet();
                } finally {
                    terminaron.countDown();
                }
            });
        }

        largada.countDown();
        terminaron.await(60, TimeUnit.SECONDS);
        hilos.shutdown();
        return resultados;
    }
}