package com.tudai.filavirtualservice.scheduler;

import static org.mockito.Mockito.verify;

import com.tudai.filavirtualservice.service.FilaVirtualService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TandasAutomaticasSchedulerTest {
    @Mock
    private FilaVirtualService filaVirtualService;

    @InjectMocks
    private TandasAutomaticasScheduler scheduler;

    @Test
    void delegaLaHabilitacionDeTandasVencidasAlServicio() {
        scheduler.habilitarTandasVencidas();

        verify(filaVirtualService).habilitarTandasVencidas();
    }
}
