package org.example.catalogoservice.config; // Ajustá el paquete según el microservicio

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Catálogo - TicketYa",
                version = "v1",
                description = """
                        Gestión de eventos, estados del ciclo de vida y recintos/lugares.
                        Requisitos de seguridad: tráfico canalizado vía API Gateway (puerto 8080),
                        CORS habilitado para el origen del Gateway y validación de parámetros de entrada.
                        (Autenticación Bearer JWT prevista para etapas posteriores / endpoints protegidos).
                        """
        )
)
@SecurityScheme(
        name = "UserIdHeader",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-User-Id",
        description = "Identificador del usuario/cliente enviado por header en la etapa actual (reemplazable por Bearer JWT en E2)."
)
@SecurityScheme(
        name = "IdempotencyKeyHeader",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "Idempotency-Key",
        description = "Clave única por operación para garantizar idempotencia en solicitudes de compra."
)
public class OpenApiConfig {
}
