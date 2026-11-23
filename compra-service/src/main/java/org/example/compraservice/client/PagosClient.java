package org.example.compraservice.client;

import org.example.compraservice.dto.PagoRequest;
import org.example.compraservice.dto.PagoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "pagos-service", url = "${pago-service.url:http://localhost:8085}/api/pagos")
public interface PagosClient {

    @PostMapping("/cobrar")
    ResponseEntity<PagoResponse> cobrar(@RequestHeader("Idempotency-Key") String clave,
                                        @RequestBody PagoRequest request);

    @GetMapping("/compra/{compraId}")
    ResponseEntity<List<PagoResponse>> pagosDeCompra(@PathVariable("compraId") Long compraId);
}