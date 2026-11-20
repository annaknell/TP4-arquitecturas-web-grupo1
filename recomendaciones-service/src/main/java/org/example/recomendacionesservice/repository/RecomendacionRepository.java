package org.example.recomendacionesservice.repository;

import org.example.recomendacionesservice.entity.Recomendacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecomendacionRepository extends MongoRepository<Recomendacion, String> {
    Optional<Recomendacion> findByEventoId(Long eventoId);
}
