package org.example.recomendacionesservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.example.recomendacionesservice.entity.ClientePreferencias;
import org.example.recomendacionesservice.dto.DestinatarioNotificacionResponse;
import org.example.recomendacionesservice.service.ClientePreferenciasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/preferencias")
@Tag(name = "Preferencias de Cliente", description = "Consulta y registro de preferencias de eventos por cliente")
public class ClientePreferenciasController {
    private final ClientePreferenciasService cpService;

    public ClientePreferenciasController(ClientePreferenciasService cpService) {
        this.cpService = cpService;
    }

    @Operation(
            summary = "Obtener preferencias por ID de cliente",
            description = "Busca y devuelve las preferencias registradas para un cliente específico.\n\n**Roles:** CLIENTE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferencias del cliente encontradas"),
            @ApiResponse(responseCode = "404", description = "No se encontraron preferencias para el cliente"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<ClientePreferencias>> obtenerPorClienteId(
            @Parameter(description = "ID del cliente", required = true) @PathVariable Long clienteId) {
        List<ClientePreferencias> preferencias = cpService.obtenerPorClienteId(clienteId);
        if (preferencias.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(preferencias);
    }

    @Operation(
            summary = "Guardar preferencias de cliente",
            description = "Crea o registra las preferencias de un cliente.\n\n**Roles:** CLIENTE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Preferencias guardadas exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de preferencias inválidos"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PostMapping
    public ResponseEntity<ClientePreferencias> guardarPreferencias(
            @RequestBody ClientePreferencias preferencias,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Email", required = false) String email) {
        if (email != null && !email.isBlank()) {
            preferencias.setEmail(email);
        }
        ClientePreferencias guardadas = cpService.guardarPreferencias(preferencias);
        return ResponseEntity.status(201).body(guardadas);
    }

    @Operation(
            summary = "[Interno] Clientes a notificar por artista y ciudad",
            description = "Devuelve los clientes que tienen al artista como favorito y la ciudad configurada. Lo usa notificaciones-service (US-40).\n\n**Acceso:** [Interno] solo entre microservicios. El API Gateway lo bloquea para clientes externos."
    )
    @SecurityRequirements
    @GetMapping("/interno/destinatarios")
    public ResponseEntity<java.util.List<DestinatarioNotificacionResponse>> buscarDestinatarios(
            @RequestParam String artista, @RequestParam String ciudad) {
        return ResponseEntity.ok(cpService.buscarDestinatarios(artista, ciudad));
    }
}