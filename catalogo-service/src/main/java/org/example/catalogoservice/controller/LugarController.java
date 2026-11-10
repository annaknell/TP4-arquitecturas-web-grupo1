package org.example.catalogoservice.controller;

import org.example.catalogoservice.entity.Lugar;
import org.example.catalogoservice.repository.LugarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lugares")
public class LugarController {

    @Autowired
    private LugarRepository lugarRepository;

    @GetMapping
    public ResponseEntity<List<Lugar>> listarLugares() {
        return ResponseEntity.ok(lugarRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Lugar> crearLugar(@RequestBody Lugar lugar) {
        Lugar nuevoLugar = lugarRepository.save(lugar);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoLugar);
    }
}