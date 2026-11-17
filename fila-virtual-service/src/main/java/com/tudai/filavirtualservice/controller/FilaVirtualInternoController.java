package com.tudai.filavirtualservice.controller;

import com.tudai.filavirtualservice.dto.ConfiguracionEventoFilaRequest;
import com.tudai.filavirtualservice.dto.EstadoEventoFilaRequest;
import com.tudai.filavirtualservice.dto.HabilitarTandaResponse;
import com.tudai.filavirtualservice.service.FilaVirtualService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints privados: Catálogo los invoca directamente, sin pasar por el Gateway. */
@RestController
@RequestMapping("/interno/fila")
public class FilaVirtualInternoController {
    private final FilaVirtualService filaVirtualService;

    public FilaVirtualInternoController(FilaVirtualService filaVirtualService) {
        this.filaVirtualService = filaVirtualService;
    }

    @PutMapping("/{eventoId}/configuracion")
    public ResponseEntity<Void> configurarEvento(@PathVariable Long eventoId,
                                                  @Valid @RequestBody ConfiguracionEventoFilaRequest request) {
        filaVirtualService.configurarEvento(eventoId, request.tamanioTanda(), request.ventanaCompraMinutos());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{eventoId}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long eventoId,
                                               @Valid @RequestBody EstadoEventoFilaRequest request) {
        filaVirtualService.cambiarEstadoEvento(eventoId, request.estado());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventoId}/tandas/habilitar")
    public ResponseEntity<HabilitarTandaResponse> habilitarSiguienteTanda(@PathVariable Long eventoId) {
        return ResponseEntity.ok(filaVirtualService.habilitarSiguienteTanda(eventoId));
    }
}
