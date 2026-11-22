package org.example.entradaservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.example.entradaservice.dto.ValidarEntradaRequest;
import org.example.entradaservice.dto.ValidarEntradaResponse;
import org.example.entradaservice.entity.Entrada;
import org.example.entradaservice.entity.SolicitudDevolucion;
import org.example.entradaservice.service.EntradaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entradas")
@Tag(name = "Entradas", description = "Emisión, consulta, cancelación y validación de entradas en la puerta")
public class EntradaController {

    @Autowired
    private EntradaService entradaService;

    @Operation(
            summary = "Emitir entradas",
            description = "Genera y emite una o más entradas a partir de la lista recibida.\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entradas emitidas correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PostMapping("/emitir")
    public ResponseEntity<List<Entrada>> emitirEntrada(@RequestBody List<Entrada> nuevasEntradas) {
        List<Entrada> emitidas = entradaService.emitirEntradas(nuevasEntradas);
        return ResponseEntity.ok(emitidas);
    }

    @Operation(
            summary = "Obtener entrada por ID",
            description = "Devuelve el detalle de una entrada según su identificador.\n\n**Roles:** CLIENTE, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrada encontrada"),
            @ApiResponse(responseCode = "404", description = "Entrada no encontrada"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Entrada> obtenerEntradaPorId(
            @Parameter(description = "ID de la entrada", required = true) @PathVariable Long id) {
        Entrada entrada = entradaService.obtenerPorId(id);
        return ResponseEntity.ok(entrada);
    }

    @Operation(
            summary = "Listar entradas por cliente",
            description = "Obtiene todas las entradas asociadas a un cliente específico.\n\n**Roles:** CLIENTE, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de entradas del cliente obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Entrada>> listarEntradasPorCliente(
            @Parameter(description = "ID del cliente", required = true) @PathVariable Long clienteId) {
        List<Entrada> entradas = entradaService.listarPorCliente(clienteId);
        return ResponseEntity.ok(entradas);
    }

    @Operation(
            summary = "Cancelar una entrada",
            description = "Cancela una entrada específica identificada por su ID.\n\n**Roles:** CLIENTE, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrada cancelada correctamente"),
            @ApiResponse(responseCode = "404", description = "Entrada no encontrada"),
            @ApiResponse(responseCode = "409", description = "La entrada no puede cancelarse en su estado actual"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Entrada> cancelarEntrada(
            @Parameter(description = "ID de la entrada", required = true) @PathVariable Long id) {
        Entrada actualizada = entradaService.cancelarEntrada(id);
        return ResponseEntity.ok(actualizada);
    }

    @Operation(
            summary = "Cancelar entradas por evento",
            description = "Cancela las entradas asociadas a un evento y retorna los IDs de las entradas afectadas.\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entradas del evento procesadas correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    @PostMapping("/eventos/{eventoId}/cancelar")
    public ResponseEntity<List<Long>> cancelarEntradasPorEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long eventoId) {
        return ResponseEntity.ok(entradaService.cancelarEntradasPorEvento(eventoId));
    }

    @Operation(
            summary = "Validar entrada en la puerta",
            description = "Valida el código QR de una entrada para el evento indicado y la marca como usada. El cambio de estado es idempotente: un segundo intento con el mismo QR se rechaza como ya utilizada.\n\n**Roles:** PERSONAL_ACCESO, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acceso autorizado (o autorizado con advertencia de sector)"),
            @ApiResponse(responseCode = "404", description = "Código QR inexistente"),
            @ApiResponse(responseCode = "409", description = "QR ya utilizado, cancelado o reintegrado"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PostMapping("/validar")
    public ResponseEntity<ValidarEntradaResponse> validarEntrada(@RequestBody ValidarEntradaRequest request) {
        ValidarEntradaResponse respuesta = entradaService.validar(request.getCodigoQr(), request.getEventoId());
        if (respuesta.isAutorizado()) {
            return ResponseEntity.ok(respuesta);
        }
        if ("INEXISTENTE".equals(respuesta.getMotivo())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }

    @Operation(
            summary = "Solicitar la devolución de una entrada",
            description = "Registra una solicitud PENDIENTE si faltan más de 72 horas para el evento.\n\n**Roles:** CLIENTE, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud registrada"),
            @ApiResponse(responseCode = "403", description = "La entrada no pertenece al cliente"),
            @ApiResponse(responseCode = "409", description = "Entrada no devolvible, fuera de plazo o con otra devolución en curso"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PostMapping("/{id}/devolucion")
    public ResponseEntity<SolicitudDevolucion> solicitarDevolucion(
            @Parameter(description = "ID de la entrada", required = true) @PathVariable Long id,
            @Parameter(description = "ID del cliente dueño de la entrada", required = true) @RequestParam Long clienteId) {
        return ResponseEntity.ok(entradaService.solicitarDevolucion(id, clienteId));
    }

    @Operation(
            summary = "Listar solicitudes de devolución",
            description = "Devuelve todas las solicitudes o las de un estado puntual (PENDIENTE, APROBADA, RECHAZADA).\n\n**Roles:** ADMINISTRADOR."
    )
    @ApiResponse(responseCode = "200", description = "Solicitudes obtenidas correctamente")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido")
    @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    @GetMapping("/devoluciones")
    public ResponseEntity<List<SolicitudDevolucion>> listarDevoluciones(
            @Parameter(description = "Estado por el que filtrar") @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(entradaService.listarDevoluciones(estado));
    }

    @Operation(
            summary = "Aprobar una devolución",
            description = "Reintegra el importe de la entrada y libera el lugar en el inventario.\n\n**Roles:** ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolución aprobada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "409", description = "La solicitud ya fue resuelta"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PatchMapping("/devoluciones/{id}/aprobar")
    public ResponseEntity<SolicitudDevolucion> aprobarDevolucion(
            @Parameter(description = "ID de la solicitud", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(entradaService.resolverDevolucion(id, true, null));
    }

    @Operation(
            summary = "Rechazar una devolución",
            description = "Marca la solicitud como RECHAZADA indicando el motivo.\n\n**Roles:** ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolución rechazada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "409", description = "La solicitud ya fue resuelta"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PatchMapping("/devoluciones/{id}/rechazar")
    public ResponseEntity<SolicitudDevolucion> rechazarDevolucion(
            @Parameter(description = "ID de la solicitud", required = true) @PathVariable Long id,
            @Parameter(description = "Motivo del rechazo", required = true) @RequestParam String motivo) {
        return ResponseEntity.ok(entradaService.resolverDevolucion(id, false, motivo));
    }

}