package org.example.catalogoservice.dto;

import java.time.LocalDateTime;

/** Sólo los cambios que requieren avisar a compradores. */
public record EventoModificacionRequest(LocalDateTime fechaInicio, LocalDateTime fechaFin, Long lugarId) {}
