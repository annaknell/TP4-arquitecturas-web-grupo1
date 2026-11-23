package org.example.catalogoservice.service;

import org.example.catalogoservice.dto.CrearSectorDto;
import org.example.catalogoservice.entity.Lugar;
import org.example.catalogoservice.entity.Sector;
import org.example.catalogoservice.repository.LugarRepository;
import org.example.catalogoservice.repository.SectorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;
    private final LugarRepository lugarRepository;

    public SectorService(SectorRepository sectorRepository, LugarRepository lugarRepository) {
        this.sectorRepository = sectorRepository;
        this.lugarRepository = lugarRepository;
    }

    @Transactional
    public Sector crearSector(CrearSectorDto dto) {
        Lugar lugar = lugarRepository.findById(dto.lugarId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lugar no encontrado con ID: " + dto.lugarId()));

        Sector sector = Sector.builder()
                .nombre(dto.nombre())
                .capacidad(dto.capacidad())
                .lugar(lugar)
                .build();

        return sectorRepository.save(sector);
    }
}