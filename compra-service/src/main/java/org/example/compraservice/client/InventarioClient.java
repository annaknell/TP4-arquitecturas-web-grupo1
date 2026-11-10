package org.example.compraservice.client;

import org.example.compraservice.dto.ReservaRequest;
import org.example.compraservice.dto.ReservaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "inventario-service", url = "http://localhost:8083/api/inventario")
public interface InventarioClient {

    @PostMapping("/reservas")
    ResponseEntity<ReservaResponse> reservarAsientos(@RequestBody ReservaRequest request);

    @PostMapping("/reservas/{id}/confirmar")
    ResponseEntity<Void> confirmarReserva(@PathVariable("id") Long reservaId);

    @PostMapping("/reservas/{id}/liberar")
    ResponseEntity<Void> liberarReserva(@PathVariable("id") Long reservaId);
}