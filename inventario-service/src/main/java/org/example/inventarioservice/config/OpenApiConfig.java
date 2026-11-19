package org.example.inventarioservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Inventario - TicketYa",
                version = "v1",
                description = """
                        Consulta de disponibilidad, reservas temporales, confirmación/liberación y aprovisionamiento de inventario.
                        Requisitos de seguridad: canalización vía API Gateway, CORS restringido al origen del Gateway,
                        validación de datos (`@Valid`) y restricción de endpoints internos/orquestación a servicios autorizados.
                        """
        )
)
public class OpenApiConfig {
}