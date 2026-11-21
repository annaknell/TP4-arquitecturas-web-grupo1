package com.tudai.filavirtualservice.controller;

import com.tudai.filavirtualservice.dto.ConfiguracionEventoFilaRequest;
import com.tudai.filavirtualservice.dto.EstadoEventoFilaRequest;
import com.tudai.filavirtualservice.dto.HabilitarTandaResponse;
import com.tudai.filavirtualservice.dto.ValidarAutorizacionRequest;
import com.tudai.filavirtualservice.dto.ValidarAutorizacionResponse;
import com.tudai.filavirtualservice.service.FilaVirtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints privados: Catálogo los invoca directamente, sin pasar por el Gateway. */
@RestController
@RequestMapping("/api/interno/fila")
@Tag(
        name = "Fila Virtual (Interno)",
        description = "Endpoints internos invocados por Catálogo sin pasar por el Gateway (requieren restricción de red/acceso interno)"
)
public class FilaVirtualInternoController {
    private final FilaVirtualService filaVirtualService;

    public FilaVirtualInternoController(FilaVirtualService filaVirtualService) {
        this.filaVirtualService = filaVirtualService;
    }

    @Operation(
            summary = "Configurar fila virtual para un evento",
            description = "Define el tamaño de tanda y la ventana de compra (en minutos) para el evento indicado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Configuración aplicada correctamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de configuración inválidos")
    })
    @PutMapping("/{eventoId}/configuracion")
    public ResponseEntity<Void> configurarEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long eventoId,
            @Valid @RequestBody ConfiguracionEventoFilaRequest request) {
        filaVirtualService.configurarEvento(eventoId, request.tamanioTanda(), request.ventanaCompraMinutos());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Cambiar estado de la fila virtual de un evento",
            description = "Actualiza el estado operativo de la fila virtual para el evento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Estado o solicitud inválida")
    })
    @PutMapping("/{eventoId}/estado")
    public ResponseEntity<Void> cambiarEstado(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long eventoId,
            @Valid @RequestBody EstadoEventoFilaRequest request) {
        filaVirtualService.cambiarEstadoEvento(eventoId, request.estado());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Habilitar siguiente tanda",
            description = "Habilita el siguiente lote/tanda de usuarios autorizados para comprar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Siguiente tanda habilitada"),
            @ApiResponse(responseCode = "409", description = "No es posible habilitar tanda en el estado actual")
    })
    @PostMapping("/{eventoId}/tandas/habilitar")
    public ResponseEntity<HabilitarTandaResponse> habilitarSiguienteTanda(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long eventoId) {
        return ResponseEntity.ok(filaVirtualService.habilitarSiguienteTanda(eventoId));
    }

    @Operation(
            summary = "Validar y consumir autorización",
            description = "Verifica el token de autorización del cliente para el evento y lo consume al iniciar la compra."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autorización validada y consumida"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Ticket/autorización no encontrada")
    })
    @PostMapping("/{eventoId}/autorizaciones/validar-y-consumir")
    public ResponseEntity<ValidarAutorizacionResponse> validarYConsumir(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long eventoId,
            @Valid @RequestBody ValidarAutorizacionRequest request) {
        return ResponseEntity.ok(filaVirtualService.validarYConsumirAutorizacion(eventoId, request.clienteId(), request.token()));
    }

    @Operation(
            summary = "Restaurar autorización",
            description = "Restaura o reintegra una autorización previamente consumida ante fallos/cancelaciones del flujo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autorización restaurada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Ticket/autorización no encontrada")
    })
    @PostMapping("/{eventoId}/autorizaciones/restaurar")
    public ResponseEntity<ValidarAutorizacionResponse> restaurar(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long eventoId,
            @Valid @RequestBody ValidarAutorizacionRequest request) {
        return ResponseEntity.ok(filaVirtualService.restaurarAutorizacion(eventoId, request.clienteId(), request.token()));
    }
}
