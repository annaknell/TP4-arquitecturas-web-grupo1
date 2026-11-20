package org.example.compraservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Recursos compartidos con notificaciones-service. */
@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "notificaciones.exchange";
    public static final String ROUTING_KEY_COMPRA_COMPLETADA = "notificaciones.evento";

    @Bean
    public TopicExchange notificacionesExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
