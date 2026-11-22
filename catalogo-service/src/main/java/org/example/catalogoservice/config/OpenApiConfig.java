package org.example.catalogoservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Catalogo - TicketYa",
                version = "v2",
                description = """
                        Gestion de eventos, estados del ciclo de vida y lugares.

                        **Seguridad (Entrega 2):** todas las requests pasan por el API Gateway (puerto 8080).
                        El cliente envia `Authorization: Bearer <token>`, obtenido en `POST /api/auth/login`.
                        El gateway valida el token y el rol, y reenvia al servicio los encabezados
                        `X-User-Id` y `X-User-Role`. Sin token o con token invalido = 401;
                        con un rol sin permiso = 403. Cada endpoint indica que roles pueden usarlo.
                        """
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT emitido por POST /api/auth/login (valido 1 hora)."
)
public class OpenApiConfig {
}