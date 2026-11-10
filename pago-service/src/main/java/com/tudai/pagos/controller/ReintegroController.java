package com.tudai.pagos.controller;

import com.tudai.pagos.dto.*;
import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.service.ReintegroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.UUID;

@RestController
@RequestMapping("/reintegros")
public class ReintegroController {
    private final ReintegroService reintegroService;

    public ReintegroController(ReintegroService reintegroService) {
        this.reintegroService = reintegroService;
    }

    @GetMapping
    public List<ReintegroResponse> getAll(@RequestParam(required = false) EstadoReintegro estado) {
        return reintegroService.getAll(estado).stream().map(ReintegroResponse::de).toList();
    }

    @GetMapping("/resumen")
    public ResumenReintegrosResponse resumen(@RequestParam Long compraId) {
        return reintegroService.resumenPorCompra(compraId);
    }

    @GetMapping("/eventos/{eventoId}/resumen")
    public ResumenReintegrosEventoResponse resumenPorEvento(@PathVariable Long eventoId) {
        return reintegroService.resumenPorEvento(eventoId);
    }

    @GetMapping("/{id}")
    public ReintegroResponse getById(@PathVariable Long id) {
        return ReintegroResponse.de(reintegroService.getById(id));
    }

    @GetMapping("/compra/{compraId}")
    public List<ReintegroResponse> getByCompraId(@PathVariable Long compraId) {
        return reintegroService.getByCompraId(compraId).stream().map(ReintegroResponse::de).toList();
    }

    @GetMapping("/devolucion/{solicitudDevolucionId}")
    public ReintegroResponse getBySolicitudDevolucionId(@PathVariable Long solicitudDevolucionId) {
        return ReintegroResponse.de(
                reintegroService.getBySolicitudDevolucionId(solicitudDevolucionId));
    }

    @GetMapping("/pago/{pagoId}")
    public List<ReintegroResponse> getByPagoId(@PathVariable Long pagoId) {
        return reintegroService.getByPagoId(pagoId).stream().map(ReintegroResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReintegroResponse reintegrar(@RequestHeader("Idempotency-Key") String clave,
                                        @Valid @RequestBody ReintegroRequest req) {
        UUID.fromString(clave);
        return ReintegroResponse.de(reintegroService.reintegrar(
                req.pagoId(), clave, req.monto(), req.motivo()));
    }

    @PostMapping("/{id}/reintentar")
    public ReintegroResponse reintentar(@PathVariable Long id) {
        return ReintegroResponse.de(reintegroService.reintentar(id));
    }
    @PostMapping("/compra/{compraId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ReintegroResponse reintegrarPorCompra(@PathVariable Long compraId,
                                                 @Valid @RequestBody ReintegroCompraRequest req) {
        return ReintegroResponse.de(reintegroService.reintegrarPorCompra(compraId, req.motivo()));
    }

    @PostMapping("/cancelacion-evento")
    public List<ReintegroResponse> registrarPendientesPorCancelacion(
            @Valid @RequestBody CancelacionEventoRequest req) {
        return req.comprasIds().stream()
                .distinct()
                .map(compraId -> reintegroService.registrarPendientePorCancelacion(compraId, req.eventoId()))
                .flatMap(java.util.Optional::stream)
                .map(ReintegroResponse::de)
                .toList();
    }

    @PostMapping("/devolucion-aprobada")
    @ResponseStatus(HttpStatus.CREATED)
    public ReintegroResponse reintegrarDevolucionAprobada(
            @Valid @RequestBody ReintegroDevolucionAprobadaRequest req) {
        return ReintegroResponse.de(reintegroService.reintegrarDevolucionAprobada(
                req.solicitudDevolucionId(), req.pagoId(), req.monto()));
    }

}
