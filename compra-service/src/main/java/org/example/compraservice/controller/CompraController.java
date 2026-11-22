package org.example.compraservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.example.compraservice.dto.CompraRequest;
import org.example.compraservice.dto.CompradorNotificacionResponse;
import org.example.compraservice.entity.Compra;
import org.example.compraservice.service.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/compras")
@Tag(name = "Compras", description = "Inicio y consulta de órdenes de compra de entradas")
public class CompraController {

    @Autowired
    private CompraService compraService;

    @Operation(
            summary = "Iniciar una compra",
            description = """
                    Procesa una solicitud de compra de forma idempotente utilizando `Idempotency-Key`.
                    El cliente se identifica con su token JWT: el gateway completa `X-User-Id` y `X-User-Email`.

                    **Roles:** CLIENTE.
                    """,
            security = {
                    @SecurityRequirement(name = "bearerAuth"),
                    @SecurityRequirement(name = "IdempotencyKeyHeader")
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compra completada exitosamente"),
            @ApiResponse(responseCode = "402", description = "Pago rechazado (`PAGO_RECHAZADO`)"),
            @ApiResponse(responseCode = "409", description = "Conflicto al procesar la compra (ej. disponibilidad/estado)"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PostMapping
    public ResponseEntity<Compra> iniciarCompra(
            @RequestBody CompraRequest request,
            @Parameter(description = "Clave única para garantizar la idempotencia de la compra", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long clienteId,
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String correlation = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
        Compra compra = compraService.procesarCompra(request, idempotencyKey, clienteId, correlation, email);

        if (compra.getEstado().equals("COMPLETADA")) {
            return ResponseEntity.status(HttpStatus.CREATED).body(compra);
        } else if (compra.getEstado().equals("PAGO_RECHAZADO")) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(compra); // 402
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(compra); // 409
        }
    }

    @Operation(
            summary = "Obtener compra por ID",
            description = "Consulta el estado y detalle de una compra existente por su identificador.\n\n**Roles:** CLIENTE, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compra encontrada"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Compra> obtenerCompra(
            @Parameter(description = "ID de la compra", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtenerPorId(id));
    }

    @Operation(
            summary = "[Interno] Compradores de un evento",
            description = "Devuelve los compradores con compras completadas de un evento. Lo usa notificaciones-service para avisar cambios o cancelaciones.\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @GetMapping("/interno/{eventoId}/compradores")
    public ResponseEntity<List<CompradorNotificacionResponse>> compradoresPorEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(compraService.compradoresCompletadosPorEvento(eventoId));
    }
}