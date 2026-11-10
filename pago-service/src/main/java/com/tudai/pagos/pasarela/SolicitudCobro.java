package com.tudai.pagos.pasarela;

public record SolicitudCobro(String claveIdempotencia, Integer monto, String moneda) {
}
