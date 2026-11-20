package com.tudai.notificacionesservice.notification;

/**
 * Puerto de salida para el proveedor de email. Permite reemplazar la implementación
 * simulada por SMTP u otro proveedor sin modificar el procesamiento de notificaciones.
 */
public interface NotificadorEmail {

    void enviar(String destinatario, String asunto, String contenido);
}
