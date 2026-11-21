package com.tudai.notificacionesservice.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Configuración de los reintentos de envíos de notificaciones. */
@Component
@Validated
@ConfigurationProperties(prefix = "notificaciones.reintentos")
public class ReintentosProperties {

    @Min(1)
    private int maximos = 3;

    @Min(1)
    private long demoraInicialSegundos = 30;

    public int getMaximos() {
        return maximos;
    }

    public void setMaximos(int maximos) {
        this.maximos = maximos;
    }

    public long getDemoraInicialSegundos() {
        return demoraInicialSegundos;
    }

    public void setDemoraInicialSegundos(long demoraInicialSegundos) {
        this.demoraInicialSegundos = demoraInicialSegundos;
    }

    /**
     * Espera exponencial: demora inicial, el doble, el cuádruple, etc.
     * El exponente se limita para evitar un desborde ante configuraciones anómalas.
     */
    public long demoraParaSiguienteIntentoSegundos(int intentosRealizados) {
        int exponente = Math.min(Math.max(intentosRealizados - 1, 0), 20);
        return demoraInicialSegundos * (1L << exponente);
    }
}
