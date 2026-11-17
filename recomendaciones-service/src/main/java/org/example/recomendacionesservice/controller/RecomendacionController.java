package org.example.recomendacionesservice.controller;

import org.example.recomendacionesservice.entity.Recomendacion;
import org.example.recomendacionesservice.service.RecomendacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recomendaciones")
public class RecomendacionController {
    private final RecomendacionService rService;

    public RecomendacionController(RecomendacionService recomendacionService) {
        this.rService = recomendacionService;
    }

    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<Recomendacion> obtenerPorEventoId(@PathVariable Long eventoId){
        return rService.obtenerRecomendacionesPorEvento(eventoId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Recomendacion> guardarRecomendacion(@RequestBody Recomendacion recomendacion){
        Recomendacion guardada = rService.guardarRecomendacion(recomendacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }
}
