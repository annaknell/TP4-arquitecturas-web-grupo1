package com.tudai.notificacionesservice.controller;

import com.tudai.notificacionesservice.dto.NotificacionResponse;
import com.tudai.notificacionesservice.entity.Notificacion;
import com.tudai.notificacionesservice.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponse> buscarPorId(@PathVariable Long id) {
        return notificacionService.buscarDetallePorId(id)
                .map(NotificacionResponse::desde)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<NotificacionResponse> listar(
            @RequestParam(required = false) Notificacion.Estado estado
    ) {
        return notificacionService.listar(estado).stream()
                .map(NotificacionResponse::desde)
                .toList();
    }

    @PostMapping("/{id}/reintentar")
    public ResponseEntity<NotificacionResponse> reintentar(@PathVariable Long id) {
        try {
            Notificacion notificacion = notificacionService.reintentarManualmente(id);
            return ResponseEntity.ok(NotificacionResponse.desde(notificacion));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).build();
        }
    }
}
