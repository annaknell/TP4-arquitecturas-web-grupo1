package com.tudai.pagos.pasarela;

public record SolicitudReintegro(String claveIdempotencia, String referencia, Integer monto) {
}
