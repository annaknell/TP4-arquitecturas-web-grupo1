package com.tudai.pagos.controller;

import com.tudai.pagos.entity.Pago;
import com.tudai.pagos.repository.PagoRepository;
import com.tudai.pagos.service.PagoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/////////ENDPOINTS
/// "/pagos" -> GET -> devuelve todos los pagos
/// "/pagos/{id}" -> GET -> devuelve un pago por id
/// "/pagos/cliente/{id}" -> GET -> devuelve todos los pagos de un cliente
/// "/pagos" -> POST -> crea un nuevo pago


@RestController("/pagos")
public class PagoController {
    private final PagoService pagoService;

    public PagoController( PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping()
    public List<Pago> getAllPagos(){
       return pagoService.getPagos();
    }

    @GetMapping("/{id}")
    public Pago getPagoById(@PathVariable Long id){
        return pagoService.getPagoById(id);
    }

    @GetMapping("/cliente/{id}")
    public List<Pago> getPagosByClienteId(@PathVariable Long id){
        return pagoService.getPagosByClienteId(id);
    }

    @PostMapping("")
    public void createPago(@RequestBody Pago pago){
        pagoService.savePago(pago);
    }

}
