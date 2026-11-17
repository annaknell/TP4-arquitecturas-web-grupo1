package com.tudai.filavirtualservice.redis;

/** Nombres de campos para los hashes Redis. */
public final class FilaRedisFields {

    private FilaRedisFields() {
    }

    public static final String ESTADO = "estado";
    public static final String TAMANIO_TANDA = "tamanioTanda";
    public static final String VENTANA_COMPRA_SEGUNDOS = "ventanaCompraSegundos";
    public static final String TICKET_ID = "ticketId";
    public static final String EVENTO_ID = "eventoId";
    public static final String CLIENTE_ID = "clienteId";
    public static final String INGRESADO_EN = "ingresadoEn";
    public static final String HABILITADO_HASTA = "habilitadoHasta";
    public static final String AUTORIZACION_TOKEN = "autorizacionToken";
}
