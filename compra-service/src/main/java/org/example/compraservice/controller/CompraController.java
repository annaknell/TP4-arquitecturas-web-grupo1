package org.example.compraservice.controller;

import org.example.compraservice.dto.CompraRequest;
import org.example.compraservice.entity.Compra;
import org.example.compraservice.service.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compras")
public class CompraController {
    @Autowired
    private CompraService compraService;

    @PostMapping
    public ResponseEntity<Compra> iniciarCompra(
        @RequestBody CompraRequest request,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestHeader("X-User-Id") Long clienteId) {
        // En E1 usan header para el usuario, en E2 seria el JWT

        Compra compra = compraService.procesarCompra(request, idempotencyKey, clienteId);

        if (compra.getEstado().equals("COMPLETADA")) {
            return ResponseEntity.status(HttpStatus.CREATED).body(compra);
        } else if (compra.getEstado().equals("PAGO_RECHAZADO")) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(compra); // 402
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(compra); // 409
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Compra> obtenerCompra(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtenerPorId(id));
    }
}
