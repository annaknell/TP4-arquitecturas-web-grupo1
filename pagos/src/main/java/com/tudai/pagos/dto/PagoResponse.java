package com.tudai.pagos.dto;

import com.tudai.pagos.entity.Pago;
import com.tudai.pagos.enums.EstadoPago;

public record PagoResponse(Long id, Long compraId, Long clienteId, Integer monto, String moneda, EstadoPago estado, String referencia, String motivoRechazo) {

    public static PagoResponse de(Pago p) {
        return new PagoResponse(p.getId(), p.getCompra_id(), p.getCliente_id(),
                p.getMonto(), p.getMoneda(), p.getEstado(), p.getReferencia(), p.getMotivoRechazo());
    }
}
