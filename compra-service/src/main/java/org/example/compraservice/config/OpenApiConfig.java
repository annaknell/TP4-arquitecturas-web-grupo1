package org.example.compraservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Compras - TicketYa",
                version = "v1",
                description = """
                        Gestión de órdenes de compra, procesamiento idempotente y orquestación con pago/inventario/entradas.
                        Requisitos de seguridad: tráfico canalizado vía API Gateway (puerto 8080),
                        CORS restringido al origen del Gateway, envío de `X-User-Id` para identificar al cliente en esta etapa
                        (prevista migración a Bearer JWT en E2) y exigencia de `Idempotency-Key` en operaciones críticas.
                        """
        )
)
@SecurityScheme(
        name = "UserIdHeader",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-User-Id",
        description = "Identificador del cliente enviado por header en la etapa actual (reemplazable por Bearer JWT en E2)."
)
@SecurityScheme(
        name = "IdempotencyKeyHeader",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "Idempotency-Key",
        description = "Clave única por operación para garantizar la idempotencia en el inicio de compras."
)
public class OpenApiConfig {
}