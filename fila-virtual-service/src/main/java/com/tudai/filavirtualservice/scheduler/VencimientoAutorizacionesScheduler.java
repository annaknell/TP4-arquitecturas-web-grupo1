package com.tudai.filavirtualservice.scheduler;

import com.tudai.filavirtualservice.service.FilaVirtualService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VencimientoAutorizacionesScheduler {
    private final FilaVirtualService filaVirtualService;

    public VencimientoAutorizacionesScheduler(FilaVirtualService filaVirtualService) {
        this.filaVirtualService = filaVirtualService;
    }

    @Scheduled(fixedDelayString = "${fila.virtual.vencimientos.fixed-delay-ms:30000}")
    public void vencerAutorizaciones() {
        filaVirtualService.vencerAutorizacionesPendientes();
    }
}
