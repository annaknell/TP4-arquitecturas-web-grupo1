package org.example.recomendacionesservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Document(collection = "recomendacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recomendacion {
    @Id
    private String id;
    private Long eventoId;
    private List<String> eventosSugeridos;
    private LocalDate ultimaActualizacion;
}
