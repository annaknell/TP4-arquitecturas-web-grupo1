package com.tudai.notificacionesservice.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificadorEmailSimulado implements NotificadorEmail {

    private static final Logger log = LoggerFactory.getLogger(NotificadorEmailSimulado.class);

    @Override
    public void enviar(String destinatario, String asunto, String contenido) {
        log.info("Email simulado enviado a={}; asunto={}; contenido={}",
                destinatario, asunto, contenido);
    }
}
