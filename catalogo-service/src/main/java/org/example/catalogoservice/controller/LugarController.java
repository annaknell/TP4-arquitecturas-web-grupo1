package org.example.catalogoservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.example.catalogoservice.entity.Lugar;
import org.example.catalogoservice.repository.LugarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lugares")
@Tag(name = "Lugares", description = "Gestión de recintos o lugares donde se celebran los eventos")
public class LugarController {

    @Autowired
    private LugarRepository lugarRepository;

    @Operation(
            summary = "Listar lugares",
            description = "Obtiene todos los lugares/recintos registrados en el catálogo.\n\n**Acceso:** público (sin token)."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Listado de lugares obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<Lugar>> listarLugares() {
        return ResponseEntity.ok(lugarRepository.findAll());
    }

    @Operation(
            summary = "Crear un lugar",
            description = "Registra un nuevo lugar/recinto en el catálogo.\n\n**Roles:** ORGANIZADOR, ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lugar creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos del lugar inválidos"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
    })
    @PostMapping
    public ResponseEntity<Lugar> crearLugar(@Valid @RequestBody Lugar lugar) {
        if (lugar.getSectores() != null) {
            lugar.getSectores().forEach(sector -> {
                sector.setLugar(lugar);
                if (sector.getAsientos() != null) {
                    sector.getAsientos().forEach(asiento -> asiento.setSector(sector));
                }
            });
        }
        Lugar nuevoLugar = lugarRepository.save(lugar);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoLugar);
    }
}