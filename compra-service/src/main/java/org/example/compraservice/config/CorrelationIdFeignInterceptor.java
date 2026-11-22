package org.example.compraservice.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Propaga el X-Correlation-Id del MDC a todas las llamadas Feign salientes,
 * para que inventario, pago, catálogo y entradas registren el mismo identificador.
 */
@Configuration
public class CorrelationIdFeignInterceptor {

    @Bean
    public RequestInterceptor correlationIdRequestInterceptor() {
        return template -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (correlationId != null && !correlationId.isBlank()) {
                template.header(CorrelationIdFilter.HEADER, correlationId);
            }
        };
    }
}
