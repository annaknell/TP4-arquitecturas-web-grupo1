package com.tudai.notificacionesservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tudai.notificacionesservice.config.ReintentosProperties;
import com.tudai.notificacionesservice.client.CompradoresClient;
import com.tudai.notificacionesservice.entity.Notificacion;
import com.tudai.notificacionesservice.notification.NotificadorEmail;
import com.tudai.notificacionesservice.repository.EventoRecibidoRepository;
import com.tudai.notificacionesservice.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private EventoRecibidoRepository eventoRecibidoRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private NotificadorEmail notificadorEmail;
    @Mock
    private CompradoresClient compradoresClient;

    private NotificacionService service;

    @BeforeEach
    void setUp() {
        ReintentosProperties reintentos = new ReintentosProperties();
        reintentos.setMaximos(3);
        reintentos.setDemoraInicialSegundos(30);
        service = new NotificacionService(
                eventoRecibidoRepository,
                notificacionRepository,
                new ObjectMapper(),
                notificadorEmail,
                reintentos,
                compradoresClient
        );
    }

    @Test
    void enviaUnaNotificacionPendienteCorrectamente() {
        Notificacion notificacion = notificacionPendiente(0);
        when(notificacionRepository.findByEstadoInAndProximoIntentoEnLessThanEqualAndIntentosLessThan(
                any(), any(), anyInt())).thenReturn(List.of(notificacion));

        service.procesarPendientes();

        assertEquals(Notificacion.Estado.ENVIADA, notificacion.getEstado());
        assertEquals(1, notificacion.getIntentos());
        assertNotNull(notificacion.getEnviadaEn());
        assertNull(notificacion.getProximoIntentoEn());
        verify(notificacionRepository).save(notificacion);
    }

    @Test
    void programaReintentoCuandoElEnvioFalla() {
        Notificacion notificacion = notificacionPendiente(0);
        when(notificacionRepository.findByEstadoInAndProximoIntentoEnLessThanEqualAndIntentosLessThan(
                any(), any(), anyInt())).thenReturn(List.of(notificacion));
        doThrow(new IllegalStateException("Proveedor no disponible"))
                .when(notificadorEmail).enviar(any(), any(), any());
        LocalDateTime antesDeProcesar = LocalDateTime.now(ZoneOffset.UTC);

        service.procesarPendientes();

        assertEquals(Notificacion.Estado.FALLIDA, notificacion.getEstado());
        assertEquals(1, notificacion.getIntentos());
        assertEquals("Proveedor no disponible", notificacion.getUltimoError());
        assertNotNull(notificacion.getProximoIntentoEn());
        assertTrue(notificacion.getProximoIntentoEn().isAfter(antesDeProcesar));
        verify(notificacionRepository).save(notificacion);
    }

    @Test
    void dejaLaNotificacionFallidaDefinitivamenteAlAgotarIntentos() {
        Notificacion notificacion = notificacionPendiente(2);
        when(notificacionRepository.findByEstadoInAndProximoIntentoEnLessThanEqualAndIntentosLessThan(
                any(), any(), anyInt())).thenReturn(List.of(notificacion));
        doThrow(new IllegalStateException("Proveedor no disponible"))
                .when(notificadorEmail).enviar(any(), any(), any());

        service.procesarPendientes();

        assertEquals(Notificacion.Estado.FALLIDA, notificacion.getEstado());
        assertEquals(3, notificacion.getIntentos());
        assertNull(notificacion.getProximoIntentoEn());
        verify(notificacionRepository).save(notificacion);
    }

    private Notificacion notificacionPendiente(int intentos) {
        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatario("cliente@example.com");
        notificacion.setAsunto("Confirmación de compra");
        notificacion.setContenido("Contenido de prueba");
        notificacion.setEstado(Notificacion.Estado.PENDIENTE);
        notificacion.setIntentos(intentos);
        notificacion.setProximoIntentoEn(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        return notificacion;
    }
}
