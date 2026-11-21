package com.tudai.reportesservice.repository;
import java.util.UUID;
import com.tudai.reportesservice.entity.EventoProcesado;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EventoProcesadoRepository extends JpaRepository<EventoProcesado, UUID> { }
