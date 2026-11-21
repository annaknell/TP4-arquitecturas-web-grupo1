package com.tudai.reportesservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Reportes - TicketYa",
                version = "v1",
                description = """
                        Consulta de reportes por evento/sector y proyección de movimientos (ventas y reintegros).
                        Seguridad y autorización: tráfico canalizado vía API Gateway (8080), CORS restringido al origen del Gateway,
                        autenticación stateless con Bearer JWT (oauth2-resource-server validado contra auth.jwk-set-uri mediante
                        JwtAuthConverter, extrayendo el ID desde el claim userId y el rol desde el claim role como ROLE_<role>).
                        Las rutas /api/reportes/** requieren rol ADMINISTRADOR u ORGANIZADOR (con control de titularidad sobre
                        eventos propios), y /api/reportes/interno/** exige rol ADMINISTRADOR.
                        """
        )
)
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT con claims userId (UUID) y role (ADMINISTRADOR u ORGANIZADOR)."
)
public class OpenApiConfig {
}
