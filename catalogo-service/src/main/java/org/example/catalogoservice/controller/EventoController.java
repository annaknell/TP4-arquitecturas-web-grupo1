package org.example.catalogoservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.catalogoservice.entity.Evento;
import org.example.catalogoservice.entity.EventoSector;
import org.example.catalogoservice.dto.EventoNotificacionResponse;
import org.example.catalogoservice.dto.EventoModificacionRequest;
import org.example.catalogoservice.service.EventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Consulta y administración del ciclo de vida de eventos y sus sectores")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @Operation(
            summary = "Listar o buscar eventos publicados",
            description = "Sin parámetros lista todos los publicados. Con parámetros filtra por texto, ciudad, participante, organizador y rango de fechas, y ordena por fecha, precio o relevancia.\n\n**Acceso:** público (sin token)."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de eventos obtenido correctamente"),
            @ApiResponse(responseCode = "400", description = "Criterio de orden inválido")
    })
    @GetMapping
    public ResponseEntity<List<Evento>> listarEventos(
            @Parameter(description = "Texto a buscar en el título o en los participantes") @RequestParam(required = false) String texto,
            @Parameter(description = "Ciudad del evento") @RequestParam(required = false) String ciudad,
            @Parameter(description = "Parte del nombre de un participante") @RequestParam(required = false) String participante,
            @Parameter(description = "ID del organizador") @RequestParam(required = false) Long organizadorId,
            @Parameter(description = "Fecha desde (ISO-8601)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @Parameter(description = "Fecha hasta (ISO-8601)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @Parameter(description = "Orden: fecha, precio o relevancia") @RequestParam(required = false) String orden) {
        boolean sinFiltros = texto == null && ciudad == null && participante == null
                && organizadorId == null && desde == null && hasta == null && orden == null;
        return ResponseEntity.ok(sinFiltros
                ? eventoService.listarEventosPublicados()
                : eventoService.buscar(texto, ciudad, participante, organizadorId, desde, hasta, orden));
    }

    @Operation(
            summary = "Obtener evento por ID",
            description = "Busca y retorna el detalle de un evento específico según su identificador.\n\n**Acceso:** público (sin token)."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento encontrado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Evento> obtenerEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id) {
        return eventoService.obtenerEventoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "[Interno] Datos del evento para notificaciones",
            description = "Uso interno: lo consume notificaciones-service para armar los avisos a compradores.\n\n**Acceso:** solo entre microservicios. El API Gateway lo bloquea para clientes externos (403)."
    )
    @SecurityRequirements
    @GetMapping("/interno/{id}/notificacion")
    public ResponseEntity<EventoNotificacionResponse> obtenerEventoParaNotificacion(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerParaNotificacion(id));
    }

    @Operation(
            summary = "Obtener sectores y precios de un evento",
            description = "Retorna los sectores asociados a un evento junto con sus configuraciones/precios.\n\n**Acceso:** público (sin token)."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sectores del evento obtenidos correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    @GetMapping("/{id}/sectores")
    public ResponseEntity<List<EventoSector>> obtenerSectoresDeEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerSectoresYPreciosPorEvento(id));
    }

    @Operation(
            summary = "Crear un nuevo evento",
            description = "Registra un evento nuevo asociado a un recinto/lugar existente (`lugarId`).\n\n**Roles:** ORGANIZADOR, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o lugar inexistente"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PostMapping
    public ResponseEntity<Evento> crearEvento(
            @RequestBody Evento evento,
            @Parameter(description = "ID del lugar donde se realizará el evento", required = true) @RequestParam Long lugarId) {
        Evento nuevoEvento = eventoService.crearEvento(evento, lugarId);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoEvento);
    }

    @Operation(
            summary = "Publicar un evento",
            description = "Cambia el estado del evento a publicado para hacerlo visible.\n\n**Roles:** ORGANIZADOR, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento publicado correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PatchMapping("/{id}/publicar")
    public ResponseEntity<Evento> publicarEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id) {
        Evento eventoPublicado = eventoService.publicarEvento(id);
        return ResponseEntity.ok(eventoPublicado);
    }

    @Operation(
            summary = "Cancelar un evento",
            description = "Marca el evento como cancelado.\n\n**Roles:** ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento cancelado correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Evento> cancelarEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ResponseEntity.ok(eventoService.cancelarEvento(id, correlation(correlationId)));
    }

    @Operation(
            summary = "Modificar un evento",
            description = "Modifica los datos de un evento. Si el evento tiene compradores, el cambio se notifica (US-02, US-39).\n\n**Roles:** ORGANIZADOR, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento modificado correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "El evento no se puede modificar en su estado actual"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Evento> modificarEvento(@PathVariable Long id, @RequestBody EventoModificacionRequest request,
                                                  @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ResponseEntity.ok(eventoService.modificarEvento(id, request, correlation(correlationId)));
    }

    @Operation(
            summary = "Suspender un evento",
            description = "Marca el evento como suspendido temporalmente.\n\n**Roles:** ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento suspendido correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PatchMapping("/{id}/suspender")
    public ResponseEntity<Evento> suspenderEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(eventoService.suspenderEvento(id));
    }

    @Operation(
            summary = "Reanudar un evento",
            description = "Reanuda un evento previamente suspendido.\n\n**Roles:** ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento reanudado correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PatchMapping("/{id}/reanudar")
    public ResponseEntity<Evento> reanudarEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(eventoService.reanudarEvento(id));
    }

    private String correlation(String correlationId) {
        return correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId;
    }
}