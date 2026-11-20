package com.tudai.notificacionesservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara la infraestructura de mensajería que usa Notificaciones.
 * RabbitMQ crea estos recursos al iniciar la aplicación si todavía no existen.
 */
@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "notificaciones.exchange";
    public static final String QUEUE_EVENTOS = "notificaciones.eventos";
    public static final String ROUTING_KEY_EVENTO = "notificaciones.evento";

    @Bean
    public TopicExchange notificacionesExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue eventosQueue() {
        return new Queue(QUEUE_EVENTOS, true);
    }

    @Bean
    public Binding eventosBinding(Queue eventosQueue, TopicExchange notificacionesExchange) {
        return BindingBuilder
                .bind(eventosQueue)
                .to(notificacionesExchange)
                .with(ROUTING_KEY_EVENTO);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
