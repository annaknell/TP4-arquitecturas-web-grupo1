package org.example.compraservice.client;

import org.example.compraservice.dto.EventoNotificacionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalogo-service", url = "${catalogo-service.url:http://localhost:8082}")
public interface CatalogoClient {

    @GetMapping("/api/eventos/interno/{eventoId}/notificacion")
    EventoNotificacionResponse obtenerParaNotificacion(@PathVariable Long eventoId);
}
