package org.example.inventario;

import org.example.inventario.dto.ProvisionDTO;
import org.example.inventario.dto.ReservaRequestDTO;
import org.example.inventario.dto.ReservaResponseDTO;
import org.example.inventario.entity.ConfiguracionEvento;
import org.example.inventario.entity.TipoSector;
import org.example.inventario.service.InventarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventarioTest {

    private static final Long EVENTO = 1L;
    private static final Long PLATEA = 10L;
    private static final Long CAMPO = 20L;
    private static final Long ULTIMO_LUGAR = 21L;
    private static final BigDecimal PRECIO = new BigDecimal("15000.00");

    @Autowired
    private InventarioService inventario;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void prepararEvento() {
        jdbcTemplate.execute("DELETE FROM evento_asiento");
        jdbcTemplate.execute("DELETE FROM zona_cupo");
        jdbcTemplate.execute("DELETE FROM reserva");
        jdbcTemplate.execute("DELETE FROM evento_config");
        inventario.provisionar(eventoDePrueba());
    }

    @Test
    void cienClientesPorElMismoAsiento() throws Exception {
        List<Callable<String>> intentos = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long cliente = 5_000L + i;
            intentos.add(() -> intentarReservar(PLATEA, List.of(101L), null, cliente));
        }

        List<String> resultados = correrEnParalelo(intentos);

        assertThat(resultados.stream().filter("OK"::equals).count()).isEqualTo(1);
        assertThat(resultados.stream().filter("ConflictoException"::equals).count()).isEqualTo(99);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM evento_asiento WHERE asiento_id = 101 AND estado = 'RETENIDO'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void cienClientesPorElUltimoLugarDeLaZona() throws Exception {
        List<Callable<String>> intentos = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long cliente = 6_000L + i;
            intentos.add(() -> intentarReservar(ULTIMO_LUGAR, null, 1, cliente));
        }

        List<String> resultados = correrEnParalelo(intentos);

        assertThat(resultados.stream().filter("OK"::equals).count()).isEqualTo(1);
        assertThat(resultados.stream().filter("ConflictoException"::equals).count()).isEqualTo(99);
        assertThat(jdbcTemplate.queryForObject("SELECT retenidos FROM zona_cupo WHERE sector_id = 21",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void segundoClienteRecibe409() throws Exception {
        String cuerpo = """
                {"clienteId":7,"eventId":1,"sectorId":10,"asientos":[102],"precioUnitario":15000.00}
                """;

        mockMvc.perform(post("/api/inventario/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventario/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.replace("\"clienteId\":7", "\"clienteId\":8")))
                .andExpect(status().isConflict());
    }

    @Test
    void reservaConElCuerpoQueMandaCompra() throws Exception {
        String cuerpo = """
                {"eventoId":1,"asientos":[103]}
                """;

        mockMvc.perform(post("/api/inventario/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM evento_asiento WHERE asiento_id = 103 AND estado = 'RETENIDO'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT sector_id FROM reserva ORDER BY id DESC LIMIT 1", Long.class)).isEqualTo(10L);
    }

    private ProvisionDTO eventoDePrueba() {
        List<ProvisionDTO.AsientoDef> asientos = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            asientos.add(new ProvisionDTO.AsientoDef(101L + i, "A", String.valueOf(i + 1)));
        }
        return new ProvisionDTO(EVENTO, ConfiguracionEvento.Estado.PUBLICADO, 6, List.of(
                new ProvisionDTO.SectorDef(PLATEA, TipoSector.NUMERADO, null, asientos),
                new ProvisionDTO.SectorDef(CAMPO, TipoSector.GENERAL, 3, null),
                new ProvisionDTO.SectorDef(ULTIMO_LUGAR, TipoSector.GENERAL, 1, null)));
    }

    private ReservaResponseDTO reservar(Long sectorId, List<Long> asientos, Integer cantidad, Long cliente) {
        return inventario.reservar(new ReservaRequestDTO(cliente, EVENTO, sectorId, asientos, cantidad, PRECIO, null))
                .getReserva();
    }

    private String intentarReservar(Long sectorId, List<Long> asientos, Integer cantidad, Long cliente) {
        try {
            reservar(sectorId, asientos, cantidad, cliente);
            return "OK";
        } catch (RuntimeException ex) {
            return ex.getClass().getSimpleName();
        }
    }

    private List<String> correrEnParalelo(List<Callable<String>> intentos) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(intentos.size());
        CyclicBarrier barrera = new CyclicBarrier(intentos.size());
        try {
            List<Future<String>> futuros = new ArrayList<>();
            for (Callable<String> intento : intentos) {
                futuros.add(pool.submit(() -> {
                    barrera.await(30, TimeUnit.SECONDS);
                    return intento.call();
                }));
            }
            List<String> resultados = new ArrayList<>();
            for (Future<String> futuro : futuros) {
                resultados.add(futuro.get(120, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }
}
