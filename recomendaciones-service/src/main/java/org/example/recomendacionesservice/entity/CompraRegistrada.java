package org.example.recomendacionesservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "compra_registrada")
@CompoundIndex(name = "cliente_evento_idx", def = "{'clienteId': 1, 'eventoId': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraRegistrada {
    @Id
    private String id;
    private Long compraId;
    private Long clienteId;
    private Long eventoId;
    private Instant registradaEn;
}