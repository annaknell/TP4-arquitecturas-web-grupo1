package org.example.inventory.controller;

import jakarta.validation.Valid;
import org.example.inventory.dto.DisponibilidadDTO;
import org.example.inventory.dto.LiberacionDTO;
import org.example.inventory.dto.ProvisionDTO;
import org.example.inventory.dto.ReservaRequestDTO;
import org.example.inventory.dto.ReservaResponseDTO;
import org.example.inventory.entity.ConfiguracionEvento;
import org.example.inventory.service.InventarioService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/eventos/{eventId}/disponibilidad")
    public ResponseEntity<DisponibilidadDTO> getAvailability(@PathVariable Long eventId,
                                                             @RequestParam(required = false) Long sectorId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(inventarioService.obtenerDisponibilidad(eventId, sectorId));
    }

    @PostMapping("/reservas")
    public ResponseEntity<ReservaResponseDTO> reserve(@Valid @RequestBody ReservaRequestDTO pedido) {
        ReservaResponseDTO.Resultado resultado = inventarioService.reservar(pedido);
        HttpStatus estado = resultado.isNueva() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(resultado.getReserva());
    }

    @PostMapping("/reservas/{id}/confirmar")
    public ResponseEntity<ReservaResponseDTO> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(inventarioService.confirmar(id));
    }

    @PostMapping("/reservas/{id}/liberar")
    public ResponseEntity<ReservaResponseDTO> release(@PathVariable UUID id,
                                                      @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(inventarioService.liberar(id, motivo));
    }

    // endpoints internos, los usa el orquestador

    @PostMapping("/eventos")
    public ResponseEntity<ProvisionDTO.Resultado> provision(@Valid @RequestBody ProvisionDTO pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.provisionar(pedido));
    }

    @PutMapping("/eventos/{eventId}/estado")
    public ResponseEntity<LiberacionDTO.Resultado> changeStatus(@PathVariable Long eventId,
                                                                @RequestParam ConfiguracionEvento.Estado estado,
                                                                @RequestParam(required = false) Integer maxEntradas) {
        return ResponseEntity.ok(inventarioService.cambiarEstado(eventId, estado, maxEntradas));
    }

    @PostMapping("/liberaciones")
    public ResponseEntity<LiberacionDTO.Resultado> releaseSold(@Valid @RequestBody LiberacionDTO pedido) {
        return ResponseEntity.ok(inventarioService.liberarVendidos(pedido));
    }
}
