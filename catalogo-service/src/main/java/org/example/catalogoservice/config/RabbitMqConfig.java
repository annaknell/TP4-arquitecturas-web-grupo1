package org.example.catalogoservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String EXCHANGE = "notificaciones.exchange";
    public static final String ROUTING_KEY_EVENTO = "notificaciones.evento";
    public static final String REPORTES_EXCHANGE = "reportes.exchange";

    @Bean
    TopicExchange notificacionesExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean
    TopicExchange reportesExchange() { return new TopicExchange(REPORTES_EXCHANGE, true, false); }

    @Bean
    MessageConverter messageConverter() { return new JacksonJsonMessageConverter(); }
}
