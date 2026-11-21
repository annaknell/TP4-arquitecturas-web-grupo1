package com.tudai.reportesservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class EventoProcesado {
    @Id private UUID eventId;
    private Instant procesadoEn = Instant.now();
    public EventoProcesado() { }
    public EventoProcesado(UUID eventId) { this.eventId = eventId; }
}
