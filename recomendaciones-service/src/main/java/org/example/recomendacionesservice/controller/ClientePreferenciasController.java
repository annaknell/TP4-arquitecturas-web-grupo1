package org.example.recomendacionesservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.recomendacionesservice.entity.ClientePreferencias;
import org.example.recomendacionesservice.service.ClientePreferenciasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            description = "Busca y devuelve las preferencias registradas para un cliente específico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferencias del cliente encontradas"),
            @ApiResponse(responseCode = "404", description = "No se encontraron preferencias para el cliente")
    })
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<ClientePreferencias> obtenerPorClienteId(
            @Parameter(description = "ID del cliente", required = true) @PathVariable Long clienteId) {
        return cpService.obtenerPorClienteId(clienteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Guardar preferencias de cliente",
            description = "Crea o registra las preferencias de un cliente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Preferencias guardadas exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de preferencias inválidos")
    })
    @PostMapping
    public ResponseEntity<ClientePreferencias> guardarPreferencias(@RequestBody ClientePreferencias preferencias) {
        ClientePreferencias guardadas = cpService.guardarPreferencias(preferencias);
        return ResponseEntity.status(201).body(guardadas);
    }
}