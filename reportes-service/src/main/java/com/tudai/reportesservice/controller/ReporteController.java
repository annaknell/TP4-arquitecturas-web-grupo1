package com.tudai.reportesservice.controller;

import com.tudai.reportesservice.dto.ReporteEventoResponse;
import com.tudai.reportesservice.dto.ReporteSectorResponse;
import com.tudai.reportesservice.entity.ReporteEvento;
import com.tudai.reportesservice.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Consultas de reportes por evento/sector y endpoints internos de proyección")
public class ReporteController {
    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    @Operation(
            summary = "Listar reportes de eventos",
            description = """
                    Retorna reportes de eventos según los permisos del usuario autenticado:
                    - ROLE_ADMINISTRADOR: puede listar todos los eventos o filtrar por organizadorId.
                    - Organizador: solo puede consultar sus propios eventos (si envía otro organizadorId, recibe 403).
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de reportes obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado (token JWT faltante o inválido)"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: no puede consultar reportes de otro organizador")
    })
    @GetMapping("/eventos")
    public List<ReporteEventoResponse> listarEventos(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "ID numérico opcional del organizador a filtrar")
            @RequestParam(required = false) Long organizadorId) {
        if (esAdministrador(authentication)) {
            return organizadorId == null ? service.listar() : service.listarPorOrganizador(organizadorId);
        }
        Long solicitante = Long.valueOf(authentication.getName());
        if (organizadorId != null && !organizadorId.equals(solicitante)) {
            throw new AccessDeniedException("No puede consultar reportes de otro organizador");
        }
        return service.listarPorOrganizador(solicitante);
    }

    @Operation(
            summary = "Obtener reporte resumen de un evento",
            description = "Devuelve el resumen consolidado de ventas y métricas del evento si el usuario es administrador o el organizador propietario.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte del evento obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado al evento de otro organizador"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    @GetMapping("/eventos/{id}")
    public ReporteEventoResponse evento(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        verificarAcceso(id, authentication);
        return service.resumen(id);
    }

    @Operation(
            summary = "Obtener reporte por sectores de un evento",
            description = "Devuelve el desglose de métricas por sector para el evento indicado (requiere rol administrador u organizador propietario).",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte por sectores obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado al evento de otro organizador"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    @GetMapping("/eventos/{id}/sectores")
    public List<ReporteSectorResponse> sectores(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        verificarAcceso(id, authentication);
        return service.sectores(id);
    }

    @Operation(
            summary = "[Interno] Configurar evento en reportes",
            description = "Crea o actualiza la configuración base de un evento y sus sectores para proyección de reportes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento configurado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de configuración inválidos")
    })
    @PutMapping("/interno/eventos/{id}")
    public ReporteEvento configurar(
            @Parameter(description = "ID del evento", required = true) @PathVariable Long id,
            @RequestBody Config request) {
        return service.configurar(id, request.organizadorId(), request.nombre(), request.moneda(), request.sectores());
    }

    @Operation(
            summary = "[Interno] Registrar venta en proyección de reportes",
            description = "Acumula el importe de una venta en el reporte del evento y sector correspondiente."
    )
    @ApiResponse(responseCode = "200", description = "Venta registrada en la proyección")
    @PostMapping("/interno/ventas")
    public void venta(@RequestBody Movimiento request) {
        service.venta(request.eventoId(), request.sectorId(), request.importeCentavos());
    }

    @Operation(
            summary = "[Interno] Registrar reintegro en proyección de reportes",
            description = "Registra un movimiento de reintegro (aplicado o pendiente) en el reporte del evento y sector."
    )
    @ApiResponse(responseCode = "200", description = "Reintegro registrado en la proyección")
    @PostMapping("/interno/reintegros")
    public void reintegro(@RequestBody Reintegro request) {
        service.reintegro(request.eventoId(), request.sectorId(), request.importeCentavos(), request.aplicado());
    }

    public record Config(Long organizadorId, String nombre, String moneda, List<ReporteService.SectorConfig> sectores) { }
    public record Movimiento(Long eventoId, Long sectorId, long importeCentavos) { }
    public record Reintegro(Long eventoId, Long sectorId, long importeCentavos, boolean aplicado) { }

    private void verificarAcceso(Long eventoId, Authentication authentication) {
        if (!esAdministrador(authentication)
                && !service.evento(eventoId).getOrganizadorId().equals(Long.valueOf(authentication.getName()))) {
            throw new AccessDeniedException("No puede consultar reportes de otro organizador");
        }
    }

    private boolean esAdministrador(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }
}
