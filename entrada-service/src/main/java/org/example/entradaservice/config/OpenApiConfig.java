package org.example.entradaservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Entradas - TicketYa",
                version = "v1",
                description = """
                        Emisión, consulta por cliente y cancelación de entradas individuales o por evento.
                        Requisitos de seguridad: acceso canalizado a través del API Gateway (puerto 8080),
                        CORS configurado para el origen del Gateway, validación de identificadores de entrada/cliente/evento
                        y control de autorización/titularidad previsto para consultas y cancelaciones (JWT/roles en E2).
                        """
        )
)
@SecurityScheme(
        name = "UserIdHeader",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-User-Id",
        description = "Identificador del usuario/operador en la etapa actual (prevista autenticación Bearer JWT y control de roles en E2)."
)
public class OpenApiConfig {
}