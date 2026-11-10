package org.example.inventarioservice.repository;

import org.example.inventario.entity.ConfiguracionEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionEventoRepository extends JpaRepository<ConfiguracionEvento, Long> {
}
