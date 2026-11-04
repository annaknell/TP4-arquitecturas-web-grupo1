package org.example.controller;

import org.example.entity.Entrada;
import org.example.repository.EntradaRepository;
import org.example.service.EntradaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entradas")
public class EntradaController {
    @Autowired
    private EntradaService entradaService;
    @PostMapping
    public ResponseEntity<Entrada> emitirEntrada(@RequestBody Entrada nuevaEntrada){
        Entrada e = entradaService.emitirEntrada(nuevaEntrada);
        return ResponseEntity.ok(e);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Entrada> obtenerEntradaPorId(@PathVariable Long id){
        Entrada entrada = entradaService.obtenerPorId(id);
        return ResponseEntity.ok(entrada);
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Entrada>> listarEntradasPorCliente(@PathVariable Long clienteId){
        List<Entrada> entradas = entradaService.listarPorCliente(clienteId);
        return ResponseEntity.ok(entradas);
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Entrada> cancelarEntrada(@PathVariable Long id){
        Entrada actualizada = entradaService.cancelarEntrada(id);
        return ResponseEntity.ok(actualizada);
    }
}
