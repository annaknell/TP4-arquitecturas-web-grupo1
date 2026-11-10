package org.example.inventario.repository;

import org.example.inventario.entity.ConfiguracionEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionEventoRepository extends JpaRepository<ConfiguracionEvento, Long> {
}
