package com.tudai.pagos.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Pagos y Reintegros - TicketYa",
                version = "v1",
                description = """
                        Procesamiento de cobros idempotentes, consultas de pagos, gestión de reintegros y simulación de pasarela.
                        Requisitos de seguridad y operación: tráfico canalizado vía API Gateway, CORS restringido al origen del Gateway,
                        claves de idempotencia (`Idempotency-Key`) para prevenir duplicados, trazabilidad con `X-Correlation-Id`
                        (validado para evitar inyección en logs y propagado en MDC/respuesta), restricción de `/pagos/mock/**`
                        a entornos de prueba y reintentos programados mediante `ReintegroScheduler`.
                        """
        )
)
@SecurityScheme(
        name = "IdempotencyKeyHeader",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "Idempotency-Key",
        description = "Clave única por operación (UUID recomendado) para garantizar idempotencia en cobros y reintegros."
)
@SecurityScheme(
        name = "CorrelationIdHeader",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Correlation-Id",
        description = "Identificador de correlación opcional (validado para prevenir log injection; si se omite, se genera automáticamente)."
)
public class OpenApiConfig {
}