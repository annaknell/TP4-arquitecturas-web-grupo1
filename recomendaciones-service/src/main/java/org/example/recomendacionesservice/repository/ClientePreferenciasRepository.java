package org.example.recomendacionesservice.repository;

import org.example.recomendacionesservice.entity.ClientePreferencias;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ClientePreferenciasRepository extends MongoRepository<ClientePreferencias, String> {
    Optional<ClientePreferencias> findByClienteId(Long clienteId);
    List<ClientePreferencias> findByArtistasFavoritosContainingIgnoreCaseAndCiudadesFrecuentesContainingIgnoreCase(
            String artista, String ciudad);
}
