package org.example.recomendacionesservice.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EventoCatalogoDto(
        Long id,
        String titulo,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String estado,
        boolean altaDemanda,
        Long organizadorId,
        Map<String, Object> lugar,
        List<ParticipanteDto> participantes
) {
    public boolean esPublicado() {
        return "PUBLICADO".equalsIgnoreCase(estado);
    }

    public String ciudad() {
        if (lugar == null) return null;
        Object c = lugar.get("ciudad");
        return c != null ? c.toString() : null;
    }

    public List<String> nombresParticipantes() {
        if (participantes == null) return List.of();
        return participantes.stream()
                .filter(Objects::nonNull)
                .map(ParticipanteDto::nombre)
                .filter(Objects::nonNull)
                .toList();
    }
}