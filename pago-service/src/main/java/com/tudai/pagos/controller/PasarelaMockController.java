package com.tudai.pagos.controller;

import com.tudai.pagos.pasarela.ModoMock;
import com.tudai.pagos.pasarela.PasarelaMock;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagos/mock")
@Tag(
        name = "Pasarela Mock (Pruebas)",
        description = "Configuración y limpieza del simulador de pasarela de pagos (uso exclusivo de pruebas/QA; deshabilitar o restringir en producción)"
)
public class PasarelaMockController {

    public record ModoDTO(ModoMock modo, Long demoraMs) {}

    private final PasarelaMock pasarela;

    public PasarelaMockController(PasarelaMock pasarela) {
        this.pasarela = pasarela;
    }

    @Operation(
            summary = "Consultar modo de pasarela mock",
            description = "Devuelve el modo actual de simulación y la demora configurada en milisegundos.\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Modo actual obtenido correctamente")
    @GetMapping("/modo")
    public ModoDTO getModo() {
        return new ModoDTO(pasarela.getModo(), pasarela.getDemoraMs());
    }

    @Operation(
            summary = "Configurar modo de pasarela mock",
            description = "Actualiza el modo de respuesta simulada y/o la demora en milisegundos de la pasarela.\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modo actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de simulación inválidos")
    })
    @PutMapping("/modo")
    public ModoDTO setModo(@RequestBody ModoDTO dto) {
        if (dto.modo() != null) pasarela.setModo(dto.modo());
        if (dto.demoraMs() != null) pasarela.setDemoraMs(dto.demoraMs());
        return getModo();
    }

    @Operation(
            summary = "Limpiar estado de pasarela mock",
            description = "Restablece/limpia el estado interno acumulado por el simulador de pasarela.\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Estado de la pasarela limpiado correctamente")
    @DeleteMapping("/estado")
    public void limpiar() {
        pasarela.limpiar();
    }
}