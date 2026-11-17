package org.example.recomendacionesservice.service;

import org.example.recomendacionesservice.entity.Recomendacion;
import org.example.recomendacionesservice.repository.RecomendacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class RecomendacionService {
    private final RecomendacionRepository rRepository;

    public RecomendacionService(RecomendacionRepository recomendacionRepository) {
        this.rRepository = recomendacionRepository;
    }

    public Optional<Recomendacion> obtenerRecomendacionesPorEvento(Long eventoId) {
        return rRepository.findByEventoId(eventoId);
    }

    public Optional<Recomendacion> obtenerPorEventoId(Long eventoId) {
        return rRepository.findByEventoId(eventoId);
    }

    public Recomendacion guardarRecomendacion(Recomendacion recomendacion) {
        if (recomendacion.getEventoId() == null || recomendacion.getEventoId() <= 0) {
            throw new IllegalArgumentException("El ID del evento no es válido.");
        }

        if (recomendacion.getUltimaActualizacion() == null) {
            recomendacion.setUltimaActualizacion(LocalDate.now());
        }

        return rRepository.save(recomendacion);
    }
}
