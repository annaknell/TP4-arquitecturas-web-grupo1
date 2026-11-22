package com.tudai.pagos.controller;

import com.tudai.pagos.dto.CobroRequest;
import com.tudai.pagos.dto.PagoResponse;
import com.tudai.pagos.service.PagoService;
import com.tudai.pagos.util.HashRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/////////ENDPOINTS
/// "/pagos" -> GET -> devuelve todos los pagos
/// "/pagos/{id}" -> GET -> devuelve un pago por id
/// "/pagos/cliente/{id}" -> GET -> devuelve todos los pagos de un cliente
/// "/pagos/cobrar" -> POST -> cobra una compra y devuelve el pago generado

@RestController
@RequestMapping("/api/pagos")
@Tag(name = "Pagos", description = "Cobro de compras de forma idempotente y consulta de pagos")
public class PagoController {
    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Operation(
            summary = "Listar todos los pagos",
            description = "Retorna el listado completo de pagos registrados.\n\n**Roles:** ADMINISTRADOR."
    )
    @ApiResponse(responseCode = "200", description = "Listado de pagos obtenido correctamente")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido")
    @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    @GetMapping
    public List<PagoResponse> getAllPagos() {
        return pagoService.getPagos().stream().map(PagoResponse::de).toList();
    }

    @Operation(
            summary = "Obtener pago por ID",
            description = "Devuelve el detalle de un pago según su identificador.\n\n**Roles:** CLIENTE, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago encontrado"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @GetMapping("/{id}")
    public PagoResponse getPagoById(
            @Parameter(description = "ID del pago", required = true) @PathVariable Long id) {
        return PagoResponse.de(pagoService.getPagoById(id));
    }

    @Operation(
            summary = "Listar pagos por cliente",
            description = "Obtiene todos los pagos asociados a un cliente.\n\n**Roles:** CLIENTE, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagos del cliente obtenidos correctamente"),
            @ApiResponse(responseCode = "404", description = "Cliente sin pagos o no encontrado"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @GetMapping("/cliente/{id}")
    public List<PagoResponse> getPagosByClienteId(
            @Parameter(description = "ID del cliente", required = true) @PathVariable Long id) {
        return pagoService.getPagosByClienteId(id).stream().map(PagoResponse::de).toList();
    }

    @Operation(
            summary = "Listar pagos por compra",
            description = "Obtiene los pagos registrados para una compra específica.\n\n**Roles:** CLIENTE, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagos de la compra obtenidos correctamente"),
            @ApiResponse(responseCode = "404", description = "Compra sin pagos o no encontrada"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @GetMapping("/compra/{compraId}")
    public List<PagoResponse> getPagosByCompraId(
            @Parameter(description = "ID de la compra", required = true) @PathVariable Long compraId) {
        return pagoService.getPagosByCompraId(compraId).stream().map(PagoResponse::de).toList();
    }

    @Operation(
            summary = "Cobrar una compra",
            description = """
                    Ejecuta un cobro de forma idempotente. Si no se envía `Idempotency-Key`, se genera un UUID automáticamente;
                    si se reutiliza la misma clave con otro payload puede devolverse conflicto/error de negocio (`422`/`409`).

                    **Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos.
                    """,
            security = @SecurityRequirement(name = "IdempotencyKeyHeader")
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pago procesado y registrado (aprobado, rechazado, incierto o reintento)"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "409", description = "Conflicto de estado o concurrencia"),
            @ApiResponse(responseCode = "422", description = "Clave de idempotencia reutilizada con datos distintos o regla de negocio")
    })
    @PostMapping("/cobrar")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponse cobrar(
            @Parameter(description = "Clave de idempotencia opcional (si se omite, se genera un UUID)")
            @RequestHeader(value = "Idempotency-Key", required = false) String clave,
            @Valid @RequestBody CobroRequest req) {
        if (clave == null || clave.isBlank()) {
            clave = java.util.UUID.randomUUID().toString();
        }
        return PagoResponse.de(pagoService.cobrar(
                clave, HashRequest.de(req),
                req.compraId(), req.clienteId(), req.monto(), req.moneda()));
    }
}