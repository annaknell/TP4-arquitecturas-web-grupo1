package com.tudai.notificacionesservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Notificaciones - TicketYa",
                version = "v1",
                description = """
                        Consulta y reintento manual de notificaciones del sistema.
                        Requisitos de seguridad: tráfico canalizado vía API Gateway,
                        CORS restringido al origen del Gateway, validación de identificadores de entrada
                        y control de acceso por rol/autenticación previsto para operaciones operativas o de reintento (JWT en E2).
                        """
        )
)
@SecurityScheme(
        name = "UserIdHeader",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-User-Id",
        description = "Identificador del usuario/operador en la etapa actual (reemplazable por Bearer JWT y control de roles en E2)."
)
public class OpenApiConfig {
}
