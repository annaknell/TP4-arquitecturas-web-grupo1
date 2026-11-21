package org.example.catalogoservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.catalogoservice.entity.Evento;
import org.example.catalogoservice.entity.EventoSector;
import org.example.catalogoservice.dto.EventoNotificacionResponse;
import org.example.catalogoservice.dto.EventoModificacionRequest;
import org.example.catalogoservice.service.EventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Consulta y administración del ciclo de vida de eventos y sus sectores")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @Operation(
            summary = "Listar eventos publicados",
            description = "Devuelve la lista de eventos actualmente publicados y disponibles para consulta."
    )
    @ApiResponse(responseCode = "200", description = "Listado de eventos publicados obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<Evento>> listarEventos() {
        return ResponseEntity.ok(eventoService.listarEventosPublicados());
    }

    @Operation(
            summary = "Obtener evento por ID",
            description = "Busca y retorna el detalle de un evento específico según su identificador."
    )
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

    @GetMapping("/interno/{id}/notificacion")
    public ResponseEntity<EventoNotificacionResponse> obtenerEventoParaNotificacion(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerParaNotificacion(id));
    }

    @Operation(
            summary = "Obtener sectores y precios de un evento",
            description = "Retorna los sectores asociados a un evento junto con sus configuraciones/precios."
    )
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
            description = "Registra un evento nuevo asociado a un recinto/lugar existente (`lugarId`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o lugar inexistente")
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
            description = "Cambia el estado del evento a publicado para hacerlo visible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento publicado correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida")
    })
    @PatchMapping("/{id}/publicar")
    public ResponseEntity<Evento> publicarEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id) {
        Evento eventoPublicado = eventoService.publicarEvento(id);
        return ResponseEntity.ok(eventoPublicado);
    }

    @Operation(
            summary = "Cancelar un evento",
            description = "Marca el evento como cancelado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento cancelado correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida")
    })
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Evento> cancelarEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ResponseEntity.ok(eventoService.cancelarEvento(id, correlation(correlationId)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Evento> modificarEvento(@PathVariable Long id, @RequestBody EventoModificacionRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ResponseEntity.ok(eventoService.modificarEvento(id, request, correlation(correlationId)));
    }

    @Operation(
            summary = "Suspender un evento",
            description = "Marca el evento como suspendido temporalmente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento suspendido correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida")
    })
    @PatchMapping("/{id}/suspender")
    public ResponseEntity<Evento> suspenderEvento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(eventoService.suspenderEvento(id));
    }

    @Operation(
            summary = "Reanudar un evento",
            description = "Reanuda un evento previamente suspendido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento reanudado correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida")
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
