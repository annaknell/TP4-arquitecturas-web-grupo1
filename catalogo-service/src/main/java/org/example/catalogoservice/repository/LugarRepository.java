package org.example.catalogoservice.repository;

import org.example.catalogoservice.entity.Lugar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LugarRepository extends JpaRepository<Lugar, Long> {
}
