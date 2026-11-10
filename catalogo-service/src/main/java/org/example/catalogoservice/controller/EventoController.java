package org.example.catalogoservice.controller;

import org.example.catalogoservice.entity.Evento;
import org.example.catalogoservice.entity.EventoSector;
import org.example.catalogoservice.service.EventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {
    @Autowired
    private EventoService eventoService;

    @GetMapping
    public ResponseEntity<List<Evento>> listarEventos() {
        return ResponseEntity.ok(eventoService.listarEventosPublicados());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evento> obtenerEvento(@PathVariable Long id) {
        return eventoService.obtenerEventoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/sectores")
    public ResponseEntity<List<EventoSector>> obtenerSectoresDeEvento(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerSectoresYPreciosPorEvento(id));
    }

    @PostMapping
    public ResponseEntity<Evento> crearEvento(@RequestBody Evento evento, @RequestParam Long lugarId) {
        Evento nuevoEvento = eventoService.crearEvento(evento, lugarId);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoEvento);
    }

    @PatchMapping("/{id}/publicar")
    public ResponseEntity<Evento> publicarEvento(@PathVariable Long id) {
        Evento eventoPublicado = eventoService.publicarEvento(id);
        return ResponseEntity.ok(eventoPublicado);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Evento> cancelarEvento(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.cancelarEvento(id));
    }
}
