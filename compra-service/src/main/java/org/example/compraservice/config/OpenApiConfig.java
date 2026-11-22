package org.example.compraservice.config;

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
                title = "API de Compras - TicketYa",
                version = "v2",
                description = """
                        Gestión de órdenes de compra, procesamiento idempotente y orquestación con pago/inventario/entradas.

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
        description = "Clave única por operación para garantizar la idempotencia en el inicio de compras."
)
public class OpenApiConfig {
}