package com.tudai.filavirtualservice.controller;

import com.tudai.filavirtualservice.dto.IngresoFilaResponse;
import com.tudai.filavirtualservice.dto.PosicionFilaResponse;
import com.tudai.filavirtualservice.service.FilaVirtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fila")
@Tag(name = "Fila Virtual", description = "Ingreso a la fila virtual de un evento y consulta de posición del cliente")
public class FilaVirtualController {

    private final FilaVirtualService filaVirtualService;

    public FilaVirtualController(FilaVirtualService filaVirtualService) {
        this.filaVirtualService = filaVirtualService;
    }

    @Operation(
            summary = "Ingresar a la fila virtual",
            description = "Registra el ingreso de un cliente a la fila virtual del evento o retorna su estado si ya estaba ingresado (`reingreso`).",
            security = @SecurityRequirement(name = "UserIdHeader")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ingreso registrado en la fila virtual"),
            @ApiResponse(responseCode = "200", description = "Cliente ya ingresado previamente (reingreso)"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o header faltante"),
            @ApiResponse(responseCode = "409", description = "Evento no habilitado para fila virtual")
    })
    @PostMapping("/{eventoId}/ingresos")
    public ResponseEntity<IngresoFilaResponse> ingresar(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long eventoId,
            @Parameter(description = "ID del cliente/usuario", required = true) @RequestHeader("X-User-Id") Long clienteId) {
        IngresoFilaResponse respuesta = filaVirtualService.ingresar(eventoId, clienteId);
        return ResponseEntity.status(respuesta.reingreso() ? HttpStatus.OK : HttpStatus.CREATED).body(respuesta);
    }

    @Operation(
            summary = "Consultar mi posición en la fila",
            description = "Devuelve la posición actual, tanda o estado del cliente en la fila virtual del evento.",
            security = @SecurityRequirement(name = "UserIdHeader")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posición consultada correctamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o header faltante"),
            @ApiResponse(responseCode = "404", description = "Ticket o registro de fila no encontrado")
    })
    @GetMapping("/{eventoId}/mi-posicion")
    public ResponseEntity<PosicionFilaResponse> consultarMiPosicion(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long eventoId,
            @Parameter(description = "ID del cliente/usuario", required = true) @RequestHeader("X-User-Id") Long clienteId) {
        return ResponseEntity.ok(filaVirtualService.consultarPosicion(eventoId, clienteId));
    }
}