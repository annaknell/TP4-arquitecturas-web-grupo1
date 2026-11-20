package org.example.recomendacionesservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Recomendaciones y Preferencias - TicketYa",
                version = "v1",
                description = """
                        Gestión de preferencias de clientes y recomendaciones de eventos.
                        Requisitos de seguridad: tráfico canalizado vía API Gateway (puerto 8080),
                        CORS restringido al origen del Gateway, validación de identificadores y payloads de entrada,
                        y control de acceso por titularidad/autenticación previsto para consultas o escrituras de usuario (JWT en E2).
                        """
        )
)
@SecurityScheme(
        name = "UserIdHeader",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-User-Id",
        description = "Identificador del usuario/cliente en la etapa actual (reemplazable por Bearer JWT en E2)."
)
public class OpenApiConfig {
}