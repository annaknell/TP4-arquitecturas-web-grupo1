package org.example.recomendacionesservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.recomendacionesservice.entity.Recomendacion;
import org.example.recomendacionesservice.service.RecomendacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recomendaciones")
@Tag(name = "Recomendaciones", description = "Consulta y registro de recomendaciones asociadas a eventos")
public class RecomendacionController {
    private final RecomendacionService rService;

    public RecomendacionController(RecomendacionService recomendacionService) {
        this.rService = recomendacionService;
    }

    @Operation(
            summary = "Obtener recomendación por ID de evento",
            description = "Retorna las recomendaciones asociadas al `eventoId` indicado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recomendación encontrada"),
            @ApiResponse(responseCode = "404", description = "No se encontraron recomendaciones para el evento")
    })
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<Recomendacion> obtenerPorEventoId(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long eventoId) {
        return rService.obtenerRecomendacionesPorEvento(eventoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Guardar recomendación",
            description = "Registra una nueva recomendación en el servicio."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recomendación creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de recomendación inválidos")
    })
    @PostMapping
    public ResponseEntity<Recomendacion> guardarRecomendacion(@RequestBody Recomendacion recomendacion) {
        Recomendacion guardada = rService.guardarRecomendacion(recomendacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }
}