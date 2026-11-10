package com.tudai.pagos.pasarela;

public interface PasarelaPagos {
    ResultadoCobro cobrar(SolicitudCobro solicitud);
    ResultadoReintegro reintegrar(SolicitudReintegro solicitud);
}
