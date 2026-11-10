package com.tudai.pagos.controller;

import com.tudai.pagos.pasarela.ModoMock;
import com.tudai.pagos.pasarela.PasarelaMock;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagos/mock")
public class PasarelaMockController {

    public record ModoDTO(ModoMock modo, Long demoraMs) {}

    private final PasarelaMock pasarela;

    public PasarelaMockController(PasarelaMock pasarela) {
        this.pasarela = pasarela;
    }

    @GetMapping("/modo")
    public ModoDTO getModo() {
        return new ModoDTO(pasarela.getModo(), pasarela.getDemoraMs());
    }

    @PutMapping("/modo")
    public ModoDTO setModo(@RequestBody ModoDTO dto) {
        if (dto.modo() != null) pasarela.setModo(dto.modo());
        if (dto.demoraMs() != null) pasarela.setDemoraMs(dto.demoraMs());
        return getModo();
    }

    @DeleteMapping("/estado")
    public void limpiar() {
        pasarela.limpiar();
    }
}
