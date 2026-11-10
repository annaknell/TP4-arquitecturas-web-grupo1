package com.tudai.pagos;

import com.tudai.pagos.entity.Pago;
import com.tudai.pagos.entity.Reintegro;
import com.tudai.pagos.controller.ReintegroController;
import com.tudai.pagos.dto.CancelacionEventoRequest;
import com.tudai.pagos.enums.EstadoPago;
import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.enums.MotivoReintegro;
import com.tudai.pagos.pasarela.ModoMock;
import com.tudai.pagos.pasarela.PasarelaMock;
import com.tudai.pagos.repository.PagoRepository;
import com.tudai.pagos.repository.ReintegroRepository;
import com.tudai.pagos.service.ReintegroScheduler;
import com.tudai.pagos.service.ReintegroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class ReintegrosCancelacionEventoIntegrationTest {

    private static final long EVENTO_ID = 900L;

    @Autowired private ReintegroController reintegroController;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private ReintegroRepository reintegroRepository;
    @Autowired private ReintegroService reintegroService;
    @Autowired private PasarelaMock pasarelaMock;

    @BeforeEach
    void limpiarEstado() {
        reintegroRepository.deleteAll();
        pagoRepository.deleteAll();
        pasarelaMock.limpiar();
        pasarelaMock.setModo(ModoMock.APROBADO);
    }

    @Test
    void cancelacionConVariasComprasRegistraUnPendientePorCompra() {
        crearPagoAprobado(101L, 500);
        crearPagoAprobado(102L, 750);

        var respuesta = reintegroController.registrarPendientesPorCancelacion(
                new CancelacionEventoRequest(EVENTO_ID, List.of(101L, 102L, 101L)));
        assertThat(respuesta).hasSize(2);

        List<Reintegro> reintegros = reintegroRepository.findByEventoId(EVENTO_ID);
        assertThat(reintegros).hasSize(2)
                .allSatisfy(reintegro -> {
                    assertThat(reintegro.getEstadoReintegro()).isEqualTo(EstadoReintegro.PENDIENTE);
                    assertThat(reintegro.getMotivoReintegro()).isEqualTo(MotivoReintegro.CANCELACION_EVENTO);
                });
        assertThat(reintegros).extracting(Reintegro::getCompraId).containsExactlyInAnyOrder(101L, 102L);
    }

    @Test
    void repetirCancelacionNoDuplicaReintegrosNiRegistroDeAuditoria(CapturedOutput output) {
        crearPagoAprobado(201L, 500);
        CancelacionEventoRequest request = new CancelacionEventoRequest(EVENTO_ID, List.of(201L));

        reintegroController.registrarPendientesPorCancelacion(request);
        reintegroController.registrarPendientesPorCancelacion(request);

        assertThat(reintegroRepository.findByEventoId(EVENTO_ID)).hasSize(1);
        assertThat(ocurrencias(output.getOut(), "op=REINTEGRO_CANCELACION_REGISTRADO")).isEqualTo(1);
    }

    @Test
    void schedulerReintentaUnReintegroLuegoDeUnaPasarelaCaida() {
        Pago pago = crearPagoAprobado(301L, 500);
        Reintegro reintegro = reintegroService.registrarPendientePorCancelacion(pago.getCompraId(), EVENTO_ID)
                .orElseThrow();

        pasarelaMock.setModo(ModoMock.TIMEOUT);
        new ReintegroScheduler(reintegroRepository, reintegroService, 0).reintentarPendientes();
        assertThat(reintegroService.getById(reintegro.getId()).getEstadoReintegro())
                .isEqualTo(EstadoReintegro.INCIERTO);

        pasarelaMock.setModo(ModoMock.APROBADO);
        new ReintegroScheduler(reintegroRepository, reintegroService, 0).reintentarPendientes();
        assertThat(reintegroService.getById(reintegro.getId()).getEstadoReintegro())
                .isEqualTo(EstadoReintegro.APROBADO);
    }

    @Test
    void resumenAdministrativoAgrupaLosEstadosCorrectamente() {
        Pago pago = crearPagoAprobado(401L, 10_000);
        crearReintegro(pago, EstadoReintegro.PENDIENTE);
        crearReintegro(pago, EstadoReintegro.INCIERTO);
        crearReintegro(pago, EstadoReintegro.APROBADO);
        crearReintegro(pago, EstadoReintegro.RECHAZADO);
        crearReintegro(pago, EstadoReintegro.FALLIDO);

        var resumen = reintegroService.resumenPorEvento(EVENTO_ID);

        assertThat(resumen.pendientes()).isEqualTo(2);
        assertThat(resumen.completados()).isEqualTo(1);
        assertThat(resumen.fallidos()).isEqualTo(2);
        assertThat(resumen.total()).isEqualTo(5);
    }

    private Pago crearPagoAprobado(Long compraId, int monto) {
        LocalDateTime ahora = LocalDateTime.now();
        return pagoRepository.save(new Pago(
                EstadoPago.APROBADO, "ARS", monto, 1L, "hash-" + compraId,
                UUID.randomUUID().toString(), compraId, ahora, ahora.plusDays(1),
                "PAGO-" + compraId, null));
    }

    private void crearReintegro(Pago pago, EstadoReintegro estado) {
        Reintegro reintegro = new Reintegro(
                pago.getId(), pago.getCompraId(), UUID.randomUUID().toString(), "hash",
                1, MotivoReintegro.CANCELACION_EVENTO, null);
        reintegro.setEventoId(EVENTO_ID);
        reintegro.setEstadoReintegro(estado);
        reintegroRepository.save(reintegro);
    }

    private static int ocurrencias(String texto, String fragmento) {
        int cantidad = 0;
        int indice = 0;
        while ((indice = texto.indexOf(fragmento, indice)) >= 0) {
            cantidad++;
            indice += fragmento.length();
        }
        return cantidad;
    }
}
