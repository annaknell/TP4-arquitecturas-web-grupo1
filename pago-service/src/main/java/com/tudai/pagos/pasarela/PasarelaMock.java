package com.tudai.pagos.pasarela;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PasarelaMock implements PasarelaPagos{
    private volatile ModoMock modo = ModoMock.APROBADO;
    private volatile long demoraMs;

    // Como una pasarela real: misma clave de idempotencia => mismo resultado definitivo
    private final Map<String, ResultadoCobro> cobros = new ConcurrentHashMap<>();
    private final Map<String, ResultadoReintegro> reintegros = new ConcurrentHashMap<>();

    public PasarelaMock(@Value("${pasarela.mock.demora-ms:5000}") long demoraMs) {
        this.demoraMs = demoraMs;
    }

    @Override
    public ResultadoCobro cobrar(SolicitudCobro s) {
        ResultadoCobro previo = cobros.get(s.claveIdempotencia());
        if (previo != null) return previo;

        ModoMock efectivo = resolverModo();
        ResultadoCobro resultado;
        switch (efectivo) {
            case RECHAZADO -> resultado = new ResultadoCobro(
                    ResultadoCobro.Estado.RECHAZADO, null, "Fondos insuficientes (mock)");
            case INCIERTO -> {
                return new ResultadoCobro(ResultadoCobro.Estado.INCIERTO, null,
                        "Respuesta incierta de la pasarela (mock)");
            }
            case TIMEOUT -> throw new PasarelaNoDisponibleException("Timeout de la pasarela (mock)");
            default -> resultado = new ResultadoCobro(
                    ResultadoCobro.Estado.APROBADO, "MOCK-" + UUID.randomUUID(), null);
        }
        // con putIfAbsent si dos hilos llegan juntos con la misma clave, gana uno solo
        ResultadoCobro existente = cobros.putIfAbsent(s.claveIdempotencia(), resultado);
        return existente != null ? existente : resultado;
    }

    @Override
    public ResultadoReintegro reintegrar(SolicitudReintegro s) {
        ResultadoReintegro previo = reintegros.get(s.claveIdempotencia());
        if (previo != null) return previo;

        ModoMock efectivo = resolverModo();
        ResultadoReintegro resultado;
        switch (efectivo) {
            case RECHAZADO -> resultado = new ResultadoReintegro(
                    ResultadoReintegro.Estado.RECHAZADO, null, "Reintegro rechazado (mock)");
            case INCIERTO -> {
                return new ResultadoReintegro(ResultadoReintegro.Estado.INCIERTO, null,
                        "Respuesta incierta de la pasarela (mock)");
            }
            case TIMEOUT -> throw new PasarelaNoDisponibleException("Timeout de la pasarela (mock)");
            default -> resultado = new ResultadoReintegro(
                    ResultadoReintegro.Estado.APROBADO, "MOCK-RF-" + UUID.randomUUID(), null);
        }
        ResultadoReintegro existente = reintegros.putIfAbsent(s.claveIdempotencia(), resultado);
        return existente != null ? existente : resultado;
    }

    /** Resuelve INTERMITENTE a un modo concreto y aplica la demora si corresponde. */
    private ModoMock resolverModo() {
        ModoMock m = modo;
        if (m == ModoMock.DEMORADO) {
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PasarelaNoDisponibleException("Interrumpido (mock)");
            }
        }
        return m;
    }

    public ModoMock getModo() { return modo; }
    public void setModo(ModoMock modo) { this.modo = modo; }
    public long getDemoraMs() { return demoraMs; }
    public void setDemoraMs(long demoraMs) { this.demoraMs = demoraMs; }
    public void limpiar() { cobros.clear(); reintegros.clear(); }
}
