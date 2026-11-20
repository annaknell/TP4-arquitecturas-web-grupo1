package com.tudai.notificacionesservice.dto;

import com.tudai.notificacionesservice.entity.Notificacion;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacionResponse(
        Long id,
        UUID eventId,
        String destinatario,
        Notificacion.Canal canal,
        Notificacion.Tipo tipo,
        String asunto,
        Notificacion.Estado estado,
        int intentos,
        LocalDateTime proximoIntentoEn,
        LocalDateTime enviadaEn,
        String ultimoError,
        LocalDateTime creadaEn
) {
    public static NotificacionResponse desde(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getEventoRecibido().getEventId(),
                notificacion.getDestinatario(),
                notificacion.getCanal(),
                notificacion.getTipo(),
                notificacion.getAsunto(),
                notificacion.getEstado(),
                notificacion.getIntentos(),
                notificacion.getProximoIntentoEn(),
                notificacion.getEnviadaEn(),
                notificacion.getUltimoError(),
                notificacion.getCreadaEn()
        );
    }
}
