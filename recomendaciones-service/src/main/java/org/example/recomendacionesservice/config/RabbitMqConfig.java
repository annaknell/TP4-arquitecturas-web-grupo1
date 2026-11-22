package org.example.recomendacionesservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "notificaciones.exchange";
    public static final String ROUTING_KEY = "notificaciones.evento";
    public static final String QUEUE = "recomendaciones.compras.completadas";

    @Bean
    public TopicExchange notificacionesExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue recomendacionesComprasQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding recomendacionesComprasBinding() {
        return BindingBuilder
                .bind(recomendacionesComprasQueue())
                .to(notificacionesExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}