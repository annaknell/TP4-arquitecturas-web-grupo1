package com.tudai.pagos.controller;

import com.tudai.pagos.dto.CobroRequest;
import com.tudai.pagos.dto.PagoResponse;
import com.tudai.pagos.entity.Pago;
import com.tudai.pagos.service.PagoService;
import com.tudai.pagos.util.HashRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/////////ENDPOINTS
/// "/pagos" -> GET -> devuelve todos los pagos
/// "/pagos/{id}" -> GET -> devuelve un pago por id
/// "/pagos/cliente/{id}" -> GET -> devuelve todos los pagos de un cliente
/// "/pagos/cobrar" -> POST -> cobra una compra y devuelve el pago generado


@RestController
@RequestMapping("/pagos")
public class PagoController {
    private final PagoService pagoService;

    public PagoController( PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping
    public List<PagoResponse> getAllPagos() {
        return pagoService.getPagos().stream().map(PagoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public PagoResponse getPagoById(@PathVariable Long id) {
        return PagoResponse.de(pagoService.getPagoById(id));
    }

    @GetMapping("/cliente/{id}")
    public List<PagoResponse> getPagosByClienteId(@PathVariable Long id) {
        return pagoService.getPagosByClienteId(id).stream().map(PagoResponse::de).toList();
    }

    @GetMapping("/compra/{compraId}")
    public List<PagoResponse> getPagosByCompraId(@PathVariable Long compraId) {
        return pagoService.getPagosByCompraId(compraId).stream().map(PagoResponse::de).toList();
    }

    @PostMapping("/cobrar")
    @ResponseStatus(HttpStatus.CREATED) // 201 para aprobado, rechazado, incierto y reintentos
    public PagoResponse cobrar(@RequestHeader("Idempotency-Key") String clave,
                               @Valid @RequestBody CobroRequest req) {
        UUID.fromString(clave); // IllegalArgumentException -> 400 si no es un UUID
        return PagoResponse.de(pagoService.cobrar(
                clave, HashRequest.de(req),
                req.compraId(), req.clienteId(), req.monto(), req.moneda()));
    }


}
