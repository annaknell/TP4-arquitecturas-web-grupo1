package com.tudai.pagos.service;

import com.tudai.pagos.config.TraceIdFilter;
import com.tudai.pagos.entity.Reintegro;
import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.repository.ReintegroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reintenta automáticamente los reintegros que quedaron PENDIENTE o INCIERTO (US-28.2).
 *
 * - Corre cada "reintegros.reintento.intervalo-ms" (30 s por defecto).
 * - Solo toma reintegros sin actualizar hace más de "antiguedad-min-seg", para no pisar
 *   uno que el request original todavía está procesando contra la pasarela.
 * - ReintegroService.reintentar() marca FALLIDO al llegar a 3 intentos; desde ahí el
 *   scheduler deja de tocarlo y queda visible para el administrador.
 * - Es seguro con varias instancias: la clave de idempotencia en la pasarela y el
 *   @Version de Reintegro evitan el doble reintegro. El que pierde la carrera cae
 *   en el catch y reintenta en la próxima vuelta.
 * - Se puede apagar con reintegros.reintento.habilitado=false (por ejemplo, en tests).
 */
@Component
@ConditionalOnProperty(name = "reintegros.reintento.habilitado", havingValue = "true", matchIfMissing = true)
public class ReintegroScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReintegroScheduler.class);
    private static final List<EstadoReintegro> REINTENTABLES =
            List.of(EstadoReintegro.PENDIENTE, EstadoReintegro.INCIERTO);

    private final ReintegroRepository reintegroRepository;
    private final ReintegroService reintegroService;
    private final long antiguedadMinimaSeg;

    public ReintegroScheduler(ReintegroRepository reintegroRepository,
                              ReintegroService reintegroService,
                              @Value("${reintegros.reintento.antiguedad-min-seg:30}") long antiguedadMinimaSeg) {
        this.reintegroRepository = reintegroRepository;
        this.reintegroService = reintegroService;
        this.antiguedadMinimaSeg = antiguedadMinimaSeg;
    }

    @Scheduled(fixedDelayString = "${reintegros.reintento.intervalo-ms:30000}")
    public void reintentarPendientes() {
        // no hay request HTTP: se genera un traceId por corrida para poder seguirla en los logs
        MDC.put(TraceIdFilter.MDC_KEY, "scheduler-" + UUID.randomUUID());
        try {
            LocalDateTime limite = LocalDateTime.now().minusSeconds(antiguedadMinimaSeg);
            List<Reintegro> candidatos = reintegroRepository
                    .findByEstadoReintegroInAndFechaActualizacionBefore(REINTENTABLES, limite);

            for (Reintegro r : candidatos) {
                try {
                    Reintegro res = reintegroService.reintentar(r.getId());
                    log.info("Reintento automático reintegro {}: {}", r.getId(), res.getEstadoReintegro());
                } catch (Exception e) {
                    // incluye ObjectOptimisticLockingFailureException si otra instancia ganó
                    log.warn("Reintento automático del reintegro {} falló: {}", r.getId(), e.getMessage());
                }
            }
        } finally {
            MDC.remove(TraceIdFilter.MDC_KEY);
        }
    }
}
