package com.tudai.pagos.pasarela;

public record ResultadoReintegro(Estado estado, String referencia, String motivo) {
    public enum Estado {
        APROBADO,
        RECHAZADO,
        PENDIENTE,
        INCIERTO,
        FALLIDO
    }
}
