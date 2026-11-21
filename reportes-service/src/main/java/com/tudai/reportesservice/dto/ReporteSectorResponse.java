package com.tudai.reportesservice.dto;
public record ReporteSectorResponse(Long sectorId,String nombre,long vendidas,long devueltas,long entradasValidas,int capacidad,double ocupacionPorcentaje,long importeBrutoCentavos,long importeReintegradoCentavos,long importePendienteCentavos,long importeNetoCentavos) {
}
