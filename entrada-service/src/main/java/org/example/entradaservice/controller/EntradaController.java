package org.example.entradaservice.controller;


import org.example.entradaservice.dto.ValidarEntradaRequest;
import org.example.entradaservice.dto.ValidarEntradaResponse;
import org.example.entradaservice.entity.Entrada;
import org.example.entradaservice.service.EntradaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entradas")
public class EntradaController {
    @Autowired
    private EntradaService entradaService;
    @PostMapping("/emitir")
    public ResponseEntity<List<Entrada>> emitirEntrada(@RequestBody List<Entrada> nuevasEntradas) {
        List<Entrada> emitidas = entradaService.emitirEntradas(nuevasEntradas);
        return ResponseEntity.ok(emitidas);
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

    @PostMapping("/eventos/{eventoId}/cancelar")
    public ResponseEntity<List<Long>> cancelarEntradasPorEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(entradaService.cancelarEntradasPorEvento(eventoId));
    }

    @PostMapping("/validar")
    public ResponseEntity<ValidarEntradaResponse> validarEntrada(@RequestBody ValidarEntradaRequest request) {
        ValidarEntradaResponse respuesta = entradaService.validar(request.getCodigoQr(), request.getEventoId());
        if (respuesta.isAutorizado()) {
            return ResponseEntity.ok(respuesta);
        }
        if ("INEXISTENTE".equals(respuesta.getMotivo())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }
}
