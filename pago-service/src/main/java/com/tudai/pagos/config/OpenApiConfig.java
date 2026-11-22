package com.tudai.pagos.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Pagos y Reintegros - TicketYa",
                version = "v2",
                description = """
                        Procesamiento de cobros idempotentes, consultas de pagos, gestión de reintegros y simulación de pasarela.

                        **Seguridad (Entrega 2):** todas las requests pasan por el API Gateway (puerto 8080).
                        El cliente envía `Authorization: Bearer <token>`, obtenido en `POST /api/auth/login`.
                        El gateway valida el token y el rol, y reenvía al servicio los encabezados
                        `X-User-Id` y `X-User-Role` (el cliente no los envía). Sin token o con token inválido → 401;
                        con un rol sin permiso → 403. Cada endpoint indica qué roles pueden usarlo.
                        Los endpoints marcados como [Interno] solo se usan entre microservicios: el gateway los bloquea.
                        """
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT emitido por POST /api/auth/login (válido 1 hora)."
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