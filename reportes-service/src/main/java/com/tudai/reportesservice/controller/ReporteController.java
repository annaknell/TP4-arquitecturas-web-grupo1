package com.tudai.reportesservice.controller;

import com.tudai.reportesservice.dto.ReporteEventoResponse;
import com.tudai.reportesservice.dto.ReporteSectorResponse;
import com.tudai.reportesservice.entity.ReporteEvento;
import com.tudai.reportesservice.service.ReporteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    @GetMapping("/eventos")
    public List<ReporteEventoResponse> listarEventos(
            Authentication authentication,
            @RequestParam(required = false) UUID organizadorId) {
        if (esAdministrador(authentication)) {
            return organizadorId == null ? service.listar() : service.listarPorOrganizador(organizadorId);
        }
        UUID solicitante = UUID.fromString(authentication.getName());
        if (organizadorId != null && !organizadorId.equals(solicitante)) {
            throw new AccessDeniedException("No puede consultar reportes de otro organizador");
        }
        return service.listarPorOrganizador(solicitante);
    }

    @GetMapping("/eventos/{id}")
    public ReporteEventoResponse evento(@PathVariable Long id, Authentication authentication) {
        verificarAcceso(id, authentication);
        return service.resumen(id);
    }

    @GetMapping("/eventos/{id}/sectores")
    public List<ReporteSectorResponse> sectores(@PathVariable Long id, Authentication authentication) {
        verificarAcceso(id, authentication);
        return service.sectores(id);
    }

    @PutMapping("/interno/eventos/{id}")
    public ReporteEvento configurar(@PathVariable Long id, @RequestBody Config request) {
        return service.configurar(id, request.organizadorId(), request.nombre(), request.moneda(), request.sectores());
    }

    @PostMapping("/interno/ventas")
    public void venta(@RequestBody Movimiento request) {
        service.venta(request.eventoId(), request.sectorId(), request.importeCentavos());
    }

    @PostMapping("/interno/reintegros")
    public void reintegro(@RequestBody Reintegro request) {
        service.reintegro(request.eventoId(), request.sectorId(), request.importeCentavos(), request.aplicado());
    }

    public record Config(UUID organizadorId, String nombre, String moneda, List<ReporteService.SectorConfig> sectores) { }
    public record Movimiento(Long eventoId, Long sectorId, long importeCentavos) { }
    public record Reintegro(Long eventoId, Long sectorId, long importeCentavos, boolean aplicado) { }

    private void verificarAcceso(Long eventoId, Authentication authentication) {
        if (!esAdministrador(authentication)
                && !service.evento(eventoId).getOrganizadorId().equals(UUID.fromString(authentication.getName()))) {
            throw new AccessDeniedException("No puede consultar reportes de otro organizador");
        }
    }

    private boolean esAdministrador(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }
}
