package com.tudai.reportesservice.dto;
import java.time.Instant;
import java.util.List;
public record ReporteEventoResponse(Long eventoId,String nombre,String moneda,long vendidas,long devueltas,long entradasValidas,int capacidad,long importeBrutoCentavos,long importeReintegradoCentavos,long importePendienteCentavos,long importeNetoCentavos,double ocupacionPorcentaje,Instant actualizadoEn,List<ReporteSectorResponse> sectores) {
}
