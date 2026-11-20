package com.tudai.pagos.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String REPORTES_EXCHANGE = "reportes.exchange";
    @Bean TopicExchange reportesExchange() { return new TopicExchange(REPORTES_EXCHANGE, true, false); }
    @Bean JacksonJsonMessageConverter messageConverter() { return new JacksonJsonMessageConverter(); }
}
