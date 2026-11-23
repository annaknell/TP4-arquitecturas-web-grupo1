package org.example.catalogoservice.controller;

import jakarta.validation.Valid;
import org.example.catalogoservice.dto.CrearSectorDto;
import org.example.catalogoservice.entity.Sector;
import org.example.catalogoservice.service.SectorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sectores")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @PostMapping
    public ResponseEntity<Sector> crearSector(@Valid @RequestBody CrearSectorDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectorService.crearSector(dto));
    }
}