package com.tudai.notificacionesservice.notification;


public interface NotificadorEmail {

    void enviar(String destinatario, String asunto, String contenido);
}
