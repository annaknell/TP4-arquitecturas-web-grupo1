package org.example.authservice.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de OpenAPI/Swagger. La seguridad Bearer se define por anotación
 * para que "Authorize" esté disponible en Swagger UI.
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT emitido por POST /api/auth/login"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("auth-service · TicketYa")
                .description("Microservicio de Identidad y Seguridad: auto-registro, login con JWT, "
                        + "alta privilegiada de roles y JWKS público para validación en el API Gateway.")
                .version("1.0.0"));
    }
}
