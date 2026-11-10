package org.example.inventarioservice.controller;

import jakarta.validation.Valid;
import org.example.inventarioservice.dto.*;
import org.example.inventarioservice.entity.ConfiguracionEvento;
import org.example.inventarioservice.service.InventarioService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/eventos/{eventId}/disponibilidad")
    public ResponseEntity<DisponibilidadDTO> obtenerDisponibilidad(@PathVariable("eventId") Long idEvento,
                                                                   @RequestParam(name = "sectorId", required = false) Long idSector) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(inventarioService.obtenerDisponibilidad(idEvento, idSector));
    }

    @PostMapping("/reservas")
    public ResponseEntity<ReservaResponseDTO> reservar(@Valid @RequestBody ReservaRequestDTO pedido) {
        ReservaResponseDTO.Resultado resultado = inventarioService.reservar(pedido);
        HttpStatus estado = resultado.isNueva() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(resultado.getReserva());
    }

    @PostMapping("/reservas/{id}/confirmar")
    public ResponseEntity<ReservaResponseDTO> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(inventarioService.confirmar(id));
    }

    @PostMapping("/reservas/{id}/liberar")
    public ResponseEntity<ReservaResponseDTO> liberar(@PathVariable Long id,
                                                      @RequestParam(name = "motivo", required = false) String motivo) {
        return ResponseEntity.ok(inventarioService.liberar(id, motivo));
    }

    // endpoints internos, los usa el orquestador

    @PostMapping("/eventos")
    public ResponseEntity<ProvisionDTO.Resultado> provisionar(@Valid @RequestBody ProvisionDTO pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.provisionar(pedido));
    }

    @PutMapping("/eventos/{eventId}/estado")
    public ResponseEntity<LiberacionDTO.Resultado> cambiarEstado(@PathVariable("eventId") Long idEvento,
                                                                 @RequestParam(name = "estado") ConfiguracionEvento.Estado estado,
                                                                 @RequestParam(name = "maxEntradas", required = false) Integer maxEntradas) {
        return ResponseEntity.ok(inventarioService.cambiarEstado(idEvento, estado, maxEntradas));
    }

    @PostMapping("/liberaciones")
    public ResponseEntity<LiberacionDTO.Resultado> liberarVendidos(@Valid @RequestBody LiberacionDTO pedido) {
        return ResponseEntity.ok(inventarioService.liberarVendidos(pedido));
    }
}
