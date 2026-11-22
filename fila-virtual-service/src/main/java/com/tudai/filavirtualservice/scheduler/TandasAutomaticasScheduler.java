package com.tudai.filavirtualservice.scheduler;

import com.tudai.filavirtualservice.service.FilaVirtualService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TandasAutomaticasScheduler {
    private final FilaVirtualService filaVirtualService;

    public TandasAutomaticasScheduler(FilaVirtualService filaVirtualService) {
        this.filaVirtualService = filaVirtualService;
    }

    @Scheduled(fixedDelayString = "${fila.virtual.tandas.fixed-delay-ms:30000}")
    public void habilitarTandasVencidas() {
        filaVirtualService.habilitarTandasVencidas();
    }
}
