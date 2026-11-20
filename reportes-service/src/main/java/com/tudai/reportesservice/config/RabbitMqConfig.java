package com.tudai.reportesservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String EXCHANGE = "reportes.exchange";
    public static final String QUEUE = "reportes.proyecciones";

    @Bean TopicExchange reportesExchange() { return new TopicExchange(EXCHANGE, true, false); }
    @Bean Queue reportesQueue() { return new Queue(QUEUE, true); }
    @Bean Binding reportesBinding() { return BindingBuilder.bind(reportesQueue()).to(reportesExchange()).with("#"); }
    @Bean JacksonJsonMessageConverter messageConverter() { return new JacksonJsonMessageConverter(); }
}
