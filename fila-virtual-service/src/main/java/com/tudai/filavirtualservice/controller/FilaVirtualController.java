package com.tudai.filavirtualservice.controller;

import com.tudai.filavirtualservice.dto.IngresoFilaResponse;
import com.tudai.filavirtualservice.dto.PosicionFilaResponse;
import com.tudai.filavirtualservice.service.FilaVirtualService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fila")
public class FilaVirtualController {

    private final FilaVirtualService filaVirtualService;

    public FilaVirtualController(FilaVirtualService filaVirtualService) {
        this.filaVirtualService = filaVirtualService;
    }

    @PostMapping("/{eventoId}/ingresos")
    public ResponseEntity<IngresoFilaResponse> ingresar(@PathVariable Long eventoId, @RequestHeader("X-User-Id") Long clienteId) {
        IngresoFilaResponse respuesta = filaVirtualService.ingresar(eventoId, clienteId);
        return ResponseEntity.status(respuesta.reingreso() ? HttpStatus.OK : HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{eventoId}/mi-posicion")
    public ResponseEntity<PosicionFilaResponse> consultarMiPosicion(@PathVariable Long eventoId, @RequestHeader("X-User-Id") Long clienteId) {
        return ResponseEntity.ok(filaVirtualService.consultarPosicion(eventoId, clienteId));
    }
}
