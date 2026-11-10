package com.tudai.pagos.dto;

import com.tudai.pagos.entity.Reintegro;
import com.tudai.pagos.enums.EstadoReintegro;
import com.tudai.pagos.enums.MotivoReintegro;

import java.time.LocalDateTime;
public record ReintegroResponse(
        Long id,
        Long pagoId,
        Long compraId,
        Long eventoId,
        Long solicitudDevolucionId,
        Integer monto,
        MotivoReintegro motivo,
        EstadoReintegro estado,
        String referencia,
        String detalle,
        Integer intentos,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {

    public static ReintegroResponse de(Reintegro r) {
        return new ReintegroResponse(
                r.getId(), r.getPagoId(), r.getCompraId(), r.getEventoId(), r.getSolicitudDevolucionId(), r.getMonto(),
                r.getMotivoReintegro(), r.getEstadoReintegro(),
                r.getReferencia(), r.getDetalle(), r.getIntentos(),
                r.getFechaCreacion(), r.getFechaActualizacion());
    }
}
