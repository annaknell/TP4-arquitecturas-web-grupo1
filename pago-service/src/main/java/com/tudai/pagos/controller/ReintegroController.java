package com.tudai.pagos.controller;

import com.tudai.pagos.dto.*;
import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.service.ReintegroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reintegros")
@Tag(name = "Reintegros", description = "Gestión de devoluciones/reintegros, reintentos y resúmenes por compra o evento")
public class ReintegroController {
    private final ReintegroService reintegroService;

    public ReintegroController(ReintegroService reintegroService) {
        this.reintegroService = reintegroService;
    }

    @Operation(
            summary = "Listar reintegros",
            description = "Devuelve todos los reintegros, opcionalmente filtrados por `estado`."
    )
    @ApiResponse(responseCode = "200", description = "Listado de reintegros obtenido correctamente")
    @GetMapping
    public List<ReintegroResponse> getAll(
            @Parameter(description = "Estado opcional para filtrar reintegros") @RequestParam(required = false) EstadoReintegro estado) {
        return reintegroService.getAll(estado).stream().map(ReintegroResponse::de).toList();
    }

    @Operation(
            summary = "Resumen de reintegros por compra",
            description = "Retorna el resumen de reintegros asociados a `compraId`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Compra o reintegros no encontrados")
    })
    @GetMapping("/resumen")
    public ResumenReintegrosResponse resumen(
            @Parameter(description = "ID de la compra", required = true) @RequestParam Long compraId) {
        return reintegroService.resumenPorCompra(compraId);
    }

    @Operation(
            summary = "Resumen de reintegros por evento",
            description = "Retorna el resumen consolidado de reintegros para el `eventoId` indicado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen por evento obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    @GetMapping("/eventos/{eventoId}/resumen")
    public ResumenReintegrosEventoResponse resumenPorEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long eventoId) {
        return reintegroService.resumenPorEvento(eventoId);
    }

    @Operation(
            summary = "Obtener reintegro por ID",
            description = "Devuelve el detalle de un reintegro según su identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reintegro encontrado"),
            @ApiResponse(responseCode = "404", description = "Reintegro no encontrado")
    })
    @GetMapping("/{id}")
    public ReintegroResponse getById(
            @Parameter(description = "ID del reintegro", required = true) @PathVariable Long id) {
        return ReintegroResponse.de(reintegroService.getById(id));
    }

    @Operation(
            summary = "Listar reintegros por compra",
            description = "Obtiene los reintegros registrados para una compra."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reintegros de la compra obtenidos correctamente"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada")
    })
    @GetMapping("/compra/{compraId}")
    public List<ReintegroResponse> getByCompraId(
            @Parameter(description = "ID de la compra", required = true) @PathVariable Long compraId) {
        return reintegroService.getByCompraId(compraId).stream().map(ReintegroResponse::de).toList();
    }

    @Operation(
            summary = "Obtener reintegro por solicitud de devolución",
            description = "Busca un reintegro vinculado a `solicitudDevolucionId`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reintegro encontrado"),
            @ApiResponse(responseCode = "404", description = "Reintegro no encontrado")
    })
    @GetMapping("/devolucion/{solicitudDevolucionId}")
    public ReintegroResponse getBySolicitudDevolucionId(
            @Parameter(description = "ID de la solicitud de devolución", required = true) @PathVariable Long solicitudDevolucionId) {
        return ReintegroResponse.de(
                reintegroService.getBySolicitudDevolucionId(solicitudDevolucionId));
    }

    @Operation(
            summary = "Listar reintegros por pago",
            description = "Obtiene todos los reintegros asociados a `pagoId`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reintegros del pago obtenidos correctamente"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    @GetMapping("/pago/{pagoId}")
    public List<ReintegroResponse> getByPagoId(
            @Parameter(description = "ID del pago", required = true) @PathVariable Long pagoId) {
        return reintegroService.getByPagoId(pagoId).stream().map(ReintegroResponse::de).toList();
    }

    @Operation(
            summary = "Registrar/ejecutar un reintegro",
            description = "Inicia un reintegro sobre un pago existente exigiendo una `Idempotency-Key` con formato UUID válido.",
            security = @SecurityRequirement(name = "IdempotencyKeyHeader")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reintegro creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o `Idempotency-Key` sin formato UUID válido"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto de estado o regla de reintegro")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReintegroResponse reintegrar(
            @Parameter(description = "Clave de idempotencia en formato UUID", required = true)
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody ReintegroRequest req) {
        UUID.fromString(clave);
        return ReintegroResponse.de(reintegroService.reintegrar(
                req.pagoId(), clave, req.monto(), req.motivo()));
    }

    @Operation(
            summary = "Reintentar un reintegro",
            description = "Reintenta procesar un reintegro previamente fallido o pendiente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reintento procesado"),
            @ApiResponse(responseCode = "404", description = "Reintegro no encontrado"),
            @ApiResponse(responseCode = "409", description = "El reintegro no puede reintentarse en su estado actual")
    })
    @PostMapping("/{id}/reintentar")
    public ReintegroResponse reintentar(
            @Parameter(description = "ID del reintegro", required = true) @PathVariable Long id) {
        return ReintegroResponse.de(reintegroService.reintentar(id));
    }

    @Operation(
            summary = "Reintegrar por compra",
            description = "Genera un reintegro asociado a la compra indicada con el motivo especificado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reintegro por compra generado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflicto al procesar el reintegro")
    })
    @PostMapping("/compra/{compraId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ReintegroResponse reintegrarPorCompra(
            @Parameter(description = "ID de la compra", required = true) @PathVariable Long compraId,
            @Valid @RequestBody ReintegroCompraRequest req) {
        return ReintegroResponse.de(reintegroService.reintegrarPorCompra(compraId, req.motivo()));
    }

    @Operation(
            summary = "Registrar reintegros pendientes por cancelación de evento",
            description = "Registra reintegros pendientes para las compras indicadas a raíz de la cancelación de un evento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reintegros pendientes registrados correctamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    })
    @PostMapping("/cancelacion-evento")
    public List<ReintegroResponse> registrarPendientesPorCancelacion(
            @Valid @RequestBody CancelacionEventoRequest req) {
        return req.comprasIds().stream()
                .distinct()
                .map(compraId -> reintegroService.registrarPendientePorCancelacion(compraId, req.eventoId()))
                .flatMap(java.util.Optional::stream)
                .map(ReintegroResponse::de)
                .toList();
    }

    @Operation(
            summary = "Reintegrar devolución aprobada",
            description = "Ejecuta el reintegro correspondiente a una solicitud de devolución aprobada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reintegro por devolución aprobada registrado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Pago o solicitud no encontrada")
    })
    @PostMapping("/devolucion-aprobada")
    @ResponseStatus(HttpStatus.CREATED)
    public ReintegroResponse reintegrarDevolucionAprobada(
            @Valid @RequestBody ReintegroDevolucionAprobadaRequest req) {
        return ReintegroResponse.de(reintegroService.reintegrarDevolucionAprobada(
                req.solicitudDevolucionId(), req.pagoId(), req.monto()));
    }
}