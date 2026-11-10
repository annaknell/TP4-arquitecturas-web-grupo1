package org.example.compraservice.client;

import org.example.compraservice.dto.EntradaRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "entrada-service", url = "http://localhost:8086/api/entradas")
public interface EntradaClient {

    @PostMapping("/emitir")
    ResponseEntity<Void> emitirEntradas(@RequestBody List<EntradaRequest> entradas);
}