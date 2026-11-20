package com.tudai.filavirtualservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Fila Virtual - TicketYa",
                version = "v1",
                description = """
                        Gestión de ingresos a fila virtual, consulta de posición, tandas y autorizaciones de compra.
                        Requisitos de seguridad: endpoints de cliente (`/fila/**`) expuestos vía API Gateway con `X-User-Id`
                        (en E2 previsto reemplazar por Bearer JWT); endpoints internos (`/interno/fila/**`) destinados a
                        comunicación servicio-a-servicio (Catálogo), por lo que deben restringirse/segmentarse de red externa en producción.
                        """
        )
)
@SecurityScheme(
        name = "UserIdHeader",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-User-Id",
        description = "Identificador del cliente en la etapa actual (reemplazable por Bearer JWT en E2)."
)
public class OpenApiConfig {
}