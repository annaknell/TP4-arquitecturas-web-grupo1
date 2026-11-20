package org.example.recomendacionesservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "cliente_preferencias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientePreferencias {
    @Id
    private String id;
    private Long clienteId;
    private List<String> tiposEventoFavoritos;
    private List<String> artistasFavoritos;
    private List<String> ciudadesFrecuentes;
}
