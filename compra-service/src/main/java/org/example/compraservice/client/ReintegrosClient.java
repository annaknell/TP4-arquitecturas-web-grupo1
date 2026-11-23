package org.example.compraservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Cliente del endpoint real de reintegros de pago-service.
 * La ruta antigua /api/pagos/{id}/reintegrar no existe.
 */
@FeignClient(name = "reintegros-client", url = "${pago-service.url:http://localhost:8085}")
public interface ReintegrosClient {

    @PostMapping("/reintegros/compra/{compraId}")
    void reintegrarPorCompra(@PathVariable("compraId") Long compraId,
                             @RequestBody Map<String, Object> body);
}
