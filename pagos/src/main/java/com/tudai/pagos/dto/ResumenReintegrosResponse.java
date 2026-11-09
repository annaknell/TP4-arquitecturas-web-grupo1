package com.tudai.pagos.dto;

public record ResumenReintegrosResponse(
        Long compraId,
        long pendientes,
        long aprobados,
        long rechazados,
        long inciertos,
        long fallidos,
        long total) {}
