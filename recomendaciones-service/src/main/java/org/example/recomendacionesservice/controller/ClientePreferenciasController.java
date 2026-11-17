package org.example.recomendacionesservice.controller;

import org.example.recomendacionesservice.entity.ClientePreferencias;
import org.example.recomendacionesservice.service.ClientePreferenciasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferencias")
public class ClientePreferenciasController {
    private final ClientePreferenciasService cpService;

    public ClientePreferenciasController(ClientePreferenciasService cpService) {
        this.cpService = cpService;
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<ClientePreferencias> obtenerPorClienteId(@PathVariable Long clienteId) {
        return cpService.obtenerPorClienteId(clienteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ClientePreferencias> guardarPreferencias(@RequestBody ClientePreferencias preferencias) {
        ClientePreferencias guardadas = cpService.guardarPreferencias(preferencias);
        return ResponseEntity.status(201).body(guardadas);
    }
}
