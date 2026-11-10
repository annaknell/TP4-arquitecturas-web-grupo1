package com.tudai.pagos.pasarela;

public record ResultadoCobro(Estado estado, String referencia, String motivo) {
    public enum Estado {
        APROBADO,
        RECHAZADO,
        INCIERTO
    }

    public boolean aprobado(){
        return estado == Estado.APROBADO;
    }
}
