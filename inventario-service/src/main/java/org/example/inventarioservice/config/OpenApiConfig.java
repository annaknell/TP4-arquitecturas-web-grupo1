package org.example.inventarioservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Inventario - TicketYa",
                version = "v2",
                description = """
                        Consulta de disponibilidad, reservas temporales, confirmación/liberación y aprovisionamiento de inventario.

                        **Seguridad (Entrega 2):** desde afuera, a través del API Gateway (puerto 8080), solo es accesible
                        la consulta de disponibilidad, que es pública. El resto de los endpoints son [Interno]: los usan
                        compras y catálogo directamente, y el gateway los bloquea para clientes externos (401/403).
                        """
        )
)
public class OpenApiConfig {
}