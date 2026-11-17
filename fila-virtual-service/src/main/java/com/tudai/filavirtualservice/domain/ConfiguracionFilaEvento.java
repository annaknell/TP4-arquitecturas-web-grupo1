package com.tudai.filavirtualservice.domain;

import java.time.Duration;

/** Configuración que se almacena en el hash de un evento. */
public record ConfiguracionFilaEvento(

        Long eventoId,
        EstadoEventoFila estado,
        int tamanioTanda,
        Duration ventanaCompra) {

    public ConfiguracionFilaEvento {
        if (eventoId == null || eventoId <= 0) {
            throw new IllegalArgumentException("El id de evento debe ser positivo");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        if (tamanioTanda <= 0) {
            throw new IllegalArgumentException("El tamaño de tanda debe ser positivo");
        }
        if (ventanaCompra == null || ventanaCompra.isNegative() || ventanaCompra.isZero()) {
            throw new IllegalArgumentException("La ventana de compra debe ser positiva");
        }
    }
}
