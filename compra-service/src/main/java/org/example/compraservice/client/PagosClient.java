package org.example.compraservice.client;

import org.example.compraservice.dto.PagoRequest;
import org.example.compraservice.dto.PagoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "pagos-service", url = "http://localhost:8085/api/pagos")
public interface PagosClient {

    @PostMapping("/cobrar")
    ResponseEntity<PagoResponse> cobrar(@RequestBody PagoRequest request);

    @PostMapping("/{id}/reintegrar")
    ResponseEntity<Void> reintegrar(@PathVariable("id") Long pagoId);
}