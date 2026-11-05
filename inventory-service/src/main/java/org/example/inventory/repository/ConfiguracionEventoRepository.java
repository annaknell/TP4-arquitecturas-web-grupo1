package org.example.inventory.repository;

import org.example.inventory.entity.ConfiguracionEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionEventoRepository extends JpaRepository<ConfiguracionEvento, Long> {
}
