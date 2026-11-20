package com.tudai.reportesservice.messaging;

import com.tudai.reportesservice.config.RabbitMqConfig;
import com.tudai.reportesservice.entity.EventoProcesado;
import com.tudai.reportesservice.repository.EventoProcesadoRepository;
import com.tudai.reportesservice.service.ReporteService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReporteEventListener {
    private final EventoProcesadoRepository procesados;
    private final ReporteService reportes;

    public ReporteEventListener(EventoProcesadoRepository procesados, ReporteService reportes) {
        this.procesados = procesados;
        this.reportes = reportes;
    }

    @Transactional
    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void recibir(ReporteEvent event) {
        if (event == null || event.eventId() == null || procesados.existsById(event.eventId())) return;
        var p = event.payload();
        if ("VENTA_CONFIRMADA".equals(event.eventType())) {
            reportes.venta(numero(p, "eventoId"), numero(p, "sectorId"), numero(p, "importeCentavos"));
        } else if ("REINTEGRO_REGISTRADO".equals(event.eventType())) {
            reportes.reintegro(numero(p, "eventoId"), numero(p, "sectorId"), numero(p, "importeCentavos"), false);
        } else if ("REINTEGRO_APLICADO".equals(event.eventType())) {
            reportes.reintegro(numero(p, "eventoId"), numero(p, "sectorId"), numero(p, "importeCentavos"), true);
        }
        procesados.save(new EventoProcesado(event.eventId()));
    }

    private long numero(java.util.Map<String, Object> payload, String campo) {
        Object valor = payload.get(campo);
        if (!(valor instanceof Number numero)) throw new IllegalArgumentException("Falta " + campo);
        return numero.longValue();
    }
}
