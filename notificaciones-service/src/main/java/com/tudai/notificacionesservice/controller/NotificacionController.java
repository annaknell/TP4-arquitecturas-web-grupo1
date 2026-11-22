package com.tudai.notificacionesservice.controller;

import com.tudai.notificacionesservice.dto.NotificacionResponse;
import com.tudai.notificacionesservice.entity.Notificacion;
import com.tudai.notificacionesservice.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Consulta y reintento manual de notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Operation(
            summary = "Buscar notificación por ID",
            description = "Devuelve el detalle de una notificación según su identificador.\n\n**Roles:** ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificación encontrada"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponse> buscarPorId(
            @Parameter(description = "ID de la notificación", required = true) @PathVariable Long id) {
        return notificacionService.buscarDetallePorId(id)
                .map(NotificacionResponse::desde)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Listar notificaciones",
            description = "Obtiene el listado de notificaciones, opcionalmente filtrado por estado.\n\n**Roles:** ADMINISTRADOR."
    )
    @ApiResponse(responseCode = "200", description = "Listado de notificaciones obtenido correctamente")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido")
    @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    @GetMapping
    public List<NotificacionResponse> listar(
            @Parameter(description = "Estado opcional para filtrar las notificaciones")
            @RequestParam(required = false) Notificacion.Estado estado
    ) {
        return notificacionService.listar(estado).stream()
                .map(NotificacionResponse::desde)
                .toList();
    }

    @Operation(
            summary = "Reintentar envío de notificación",
            description = "Ejecuta un reintento manual para la notificación indicada.\n\n**Roles:** ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reintento procesado correctamente"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada"),
            @ApiResponse(responseCode = "409", description = "La notificación no puede reintentarse en su estado actual"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PostMapping("/{id}/reintentar")
    public ResponseEntity<NotificacionResponse> reintentar(
            @Parameter(description = "ID de la notificación", required = true) @PathVariable Long id) {
        try {
            Notificacion notificacion = notificacionService.reintentarManualmente(id);
            return ResponseEntity.ok(NotificacionResponse.desde(notificacion));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).build();
        }
    }
}