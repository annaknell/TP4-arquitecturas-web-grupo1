package com.tudai.reportesservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String EXCHANGE = "reportes.exchange";
    public static final String QUEUE = "reportes.proyecciones";
    public static final String DLX = "reportes.dlx";
    public static final String DLQ = "reportes.proyecciones.dlq";

    @Bean TopicExchange reportesExchange() { return new TopicExchange(EXCHANGE, true, false); }
    @Bean Queue reportesQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();
    }
    @Bean Binding reportesBinding() { return BindingBuilder.bind(reportesQueue()).to(reportesExchange()).with("#"); }
    @Bean TopicExchange reportesDlx() { return new TopicExchange(DLX, true, false); }
    @Bean Queue reportesDlq() { return QueueBuilder.durable(DLQ).build(); }
    @Bean Binding reportesDlqBinding() { return BindingBuilder.bind(reportesDlq()).to(reportesDlx()).with(DLQ); }
    @Bean JacksonJsonMessageConverter messageConverter() { return new JacksonJsonMessageConverter(); }
}
