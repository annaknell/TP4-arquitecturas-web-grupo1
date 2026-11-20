package com.tudai.notificacionesservice.scheduler;

import com.tudai.notificacionesservice.service.NotificacionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Ejecuta periódicamente la generación y el envío de notificaciones. */
@Component
public class NotificacionScheduler {

    private final NotificacionService notificacionService;

    public NotificacionScheduler(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Scheduled(fixedDelayString = "${notificaciones.procesamiento.intervalo-ms:10000}")
    public void procesarNotificaciones() {
        notificacionService.crearPendientesDesdeEventos();
        notificacionService.procesarPendientes();
    }
}
