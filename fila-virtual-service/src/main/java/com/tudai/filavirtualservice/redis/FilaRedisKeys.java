package com.tudai.filavirtualservice.redis;

/**
 * Único lugar donde se construyen las claves Redis de Fila Virtual.
 * El prefijo evita colisiones con otros microservicios que compartan la instancia.
 */
public final class FilaRedisKeys {

    private static final String PREFIX = "fila-virtual:evento:";

    private FilaRedisKeys() {
    }

    /** ZSET: clienteId -> instante de ingreso, ordenado por hora del servidor. */
    public static String cola(Long eventoId) {
        return base(eventoId) + ":cola";
    }

    /** HASH: estado y parámetros de habilitación del evento. */
    public static String configuracion(Long eventoId) {
        return base(eventoId) + ":configuracion";
    }

    /** HASH: ticket y estado de un cliente para un evento. */
    public static String ticket(Long eventoId, Long clienteId) {
        validarCliente(clienteId);
        return base(eventoId) + ":ticket:" + clienteId;
    }

    /** HASH con la autorización opaca creada al habilitar la compra (US-32). */
    public static String autorizacion(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token de autorización es obligatorio");
        }
        return "fila-virtual:autorizacion:" + token;
    }

    /** ZSET global: token -> instante de vencimiento; se usa en la siguiente etapa para expirar habilitaciones. */
    public static String vencimientosAutorizacion() {
        return "fila-virtual:autorizaciones:vencimientos";
    }

    static String prefijoTicket(Long eventoId) {
        return base(eventoId) + ":ticket:";
    }

    static String prefijoAutorizacion() {
        return "fila-virtual:autorizacion:";
    }

    static String prefijoEventos() {
        return PREFIX;
    }

    private static String base(Long eventoId) {
        if (eventoId == null || eventoId <= 0) {
            throw new IllegalArgumentException("El id de evento debe ser positivo");
        }
        return PREFIX + eventoId;
    }

    private static void validarCliente(Long clienteId) {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("El id de cliente debe ser positivo");
        }
    }
}
