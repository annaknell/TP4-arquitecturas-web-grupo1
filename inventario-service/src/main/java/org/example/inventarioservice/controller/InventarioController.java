package org.example.inventarioservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.example.inventarioservice.dto.*;
import org.example.inventarioservice.entity.ConfiguracionEvento;
import org.example.inventarioservice.service.InventarioService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario")
@Tag(name = "Inventario", description = "Disponibilidad de localidades, reservas y operaciones internas de orquestación")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @Operation(
            summary = "Consultar disponibilidad de un evento",
            description = "Retorna la disponibilidad actual del evento (opcionalmente filtrada por `sectorId`) sin caché (`no-store`).\n\n**Acceso:** público (sin token)."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad obtenida correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento o sector no encontrado")
    })
    @GetMapping("/eventos/{eventId}/disponibilidad")
    public ResponseEntity<DisponibilidadDTO> obtenerDisponibilidad(
            @Parameter(description = "ID del evento", required = true) @PathVariable("eventId") Long idEvento,
            @Parameter(description = "ID opcional del sector para filtrar disponibilidad") @RequestParam(name = "sectorId", required = false) Long idSector) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(inventarioService.obtenerDisponibilidad(idEvento, idSector));
    }

    @Operation(
            summary = "Crear o recuperar una reserva temporal",
            description = "Reserva cupo/asientos para una solicitud de compra. Devuelve `201` si se crea una nueva reserva o `200` si ya existía.\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada exitosamente"),
            @ApiResponse(responseCode = "200", description = "Reserva existente retornada"),
            @ApiResponse(responseCode = "400", description = "Datos de reserva inválidos"),
            @ApiResponse(responseCode = "409", description = "Sin disponibilidad o conflicto de estado")
    })
    @PostMapping("/reservas")
    public ResponseEntity<ReservaResponseDTO> reservar(@Valid @RequestBody ReservaRequestDTO pedido) {
        ReservaResponseDTO.Resultado resultado = inventarioService.reservar(pedido);
        HttpStatus estado = resultado.isNueva() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(resultado.getReserva());
    }

    @Operation(
            summary = "Confirmar una reserva",
            description = "Confirma definitivamente una reserva tras completarse el pago/compra.\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva confirmada correctamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "409", description = "La reserva no puede confirmarse en su estado actual")
    })
    @PostMapping("/reservas/{id}/confirmar")
    public ResponseEntity<ReservaResponseDTO> confirmar(
            @Parameter(description = "ID de la reserva", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(inventarioService.confirmar(id));
    }

    @Operation(
            summary = "Liberar una reserva",
            description = "Libera una reserva pendiente/activa (por vencimiento, fallo de pago o cancelación) indicando un motivo opcional.\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva liberada correctamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "409", description = "La reserva no puede liberarse en su estado actual")
    })
    @PostMapping("/reservas/{id}/liberar")
    public ResponseEntity<ReservaResponseDTO> liberar(
            @Parameter(description = "ID de la reserva", required = true) @PathVariable Long id,
            @Parameter(description = "Motivo opcional de la liberación") @RequestParam(name = "motivo", required = false) String motivo) {
        return ResponseEntity.ok(inventarioService.liberar(id, motivo));
    }

    // endpoints internos, los usa el orquestador

    @Operation(
            summary = "[Interno/Orquestador] Provisionar inventario de evento",
            description = "Inicializa y aprovisiona el inventario de localidades/sectores para un evento.\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inventario provisionado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de provisión inválidos")
    })
    @PostMapping("/eventos")
    public ResponseEntity<ProvisionDTO.Resultado> provisionar(@Valid @RequestBody ProvisionDTO pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.provisionar(pedido));
    }

    @Operation(
            summary = "[Interno/Orquestador] Cambiar estado de inventario del evento",
            description = "Actualiza el estado de configuración del evento en inventario y opcionalmente el límite `maxEntradas`.\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de inventario actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    @PutMapping("/eventos/{eventId}/estado")
    public ResponseEntity<LiberacionDTO.Resultado> cambiarEstado(
            @Parameter(description = "ID del evento", required = true) @PathVariable("eventId") Long idEvento,
            @Parameter(description = "Nuevo estado del evento", required = true) @RequestParam(name = "estado") ConfiguracionEvento.Estado estado,
            @Parameter(description = "Cantidad máxima de entradas permitidas (opcional)") @RequestParam(name = "maxEntradas", required = false) Integer maxEntradas) {
        return ResponseEntity.ok(inventarioService.cambiarEstado(idEvento, estado, maxEntradas));
    }

    @Operation(
            summary = "[Interno/Orquestador] Liberar entradas vendidas",
            description = "Libera/revierte inventario asociado a entradas vendidas (por ejemplo ante cancelaciones o devoluciones).\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liberación procesada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de liberación inválidos")
    })
    @PostMapping("/liberaciones")
    public ResponseEntity<LiberacionDTO.Resultado> liberarVendidos(@Valid @RequestBody LiberacionDTO pedido) {
        return ResponseEntity.ok(inventarioService.liberarVendidos(pedido));
    }
}