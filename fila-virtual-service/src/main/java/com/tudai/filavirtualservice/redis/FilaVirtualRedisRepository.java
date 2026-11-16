package com.tudai.filavirtualservice.redis;

import com.tudai.filavirtualservice.domain.ConfiguracionFilaEvento;
import com.tudai.filavirtualservice.domain.EstadoEventoFila;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class FilaVirtualRedisRepository {

    private static final DefaultRedisScript<List> INGRESAR_SCRIPT = new DefaultRedisScript<>("""
            local estadoEvento = redis.call('HGET', KEYS[1], 'estado')
            if estadoEvento ~= 'ABIERTA' then
                return {'EVENTO_NO_HABILITADO'}
            end
            local ticketExistente = redis.call('HGET', KEYS[3], 'ticketId')
            if ticketExistente then
                return {'REINGRESO', ticketExistente, redis.call('HGET', KEYS[3], 'estado'), redis.call('HGET', KEYS[3], 'ingresadoEn')}
            end
            local ahora = redis.call('TIME')
            local ahoraMillis = ahora[1] * 1000 + math.floor(ahora[2] / 1000)
            redis.call('ZADD', KEYS[2], ahoraMillis, ARGV[1])
            redis.call('HSET', KEYS[3],
                'ticketId', ARGV[2],
                'eventoId', ARGV[3],
                'clienteId', ARGV[1],
                'estado', 'EN_ESPERA',
                'ingresadoEn', ahoraMillis)
            return {'NUEVO', ARGV[2], 'EN_ESPERA', tostring(ahoraMillis)}
            """, List.class);

    private final StringRedisTemplate redis;

    public FilaVirtualRedisRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Operación atómica: verifica el evento, evita duplicados y agrega a la cola. */
    public ResultadoIngreso ingresar(Long eventoId, Long clienteId, String nuevoTicketId) {
        List resultado = redis.execute(INGRESAR_SCRIPT,
                List.of(FilaRedisKeys.configuracion(eventoId), FilaRedisKeys.cola(eventoId), FilaRedisKeys.ticket(eventoId, clienteId)),
                clienteId.toString(), nuevoTicketId, eventoId.toString());

        if (resultado == null || resultado.isEmpty()) {
            throw new IllegalStateException("Redis no devolvió resultado al registrar el ingreso");
        }
        if ("EVENTO_NO_HABILITADO".equals(texto(resultado.getFirst()))) {
            return ResultadoIngreso.eventoNoHabilitado();
        }
        return new ResultadoIngreso(
                "REINGRESO".equals(texto(resultado.getFirst())),
                texto(resultado.get(1)), texto(resultado.get(2)), Long.parseLong(texto(resultado.get(3))));
    }

    public Optional<TicketAlmacenado> buscarTicket(Long eventoId, Long clienteId) {
        Map<Object, Object> datos = redis.opsForHash().entries(FilaRedisKeys.ticket(eventoId, clienteId));
        if (datos.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TicketAlmacenado(
                texto(datos.get(FilaRedisFields.TICKET_ID)),
                texto(datos.get(FilaRedisFields.ESTADO)),
                Long.parseLong(texto(datos.get(FilaRedisFields.INGRESADO_EN))),
                numeroOpcional(datos.get(FilaRedisFields.HABILITADO_HASTA))));
    }

    public Long obtenerRango(Long eventoId, Long clienteId) {
        return redis.opsForZSet().rank(FilaRedisKeys.cola(eventoId), clienteId.toString());
    }

    /** Punto de integración para el futuro consumidor de eventos de Catálogo. */
    public void guardarConfiguracion(ConfiguracionFilaEvento configuracion) {
        redis.opsForHash().put(FilaRedisKeys.configuracion(configuracion.eventoId()), FilaRedisFields.ESTADO,
                configuracion.estado().name());
        redis.opsForHash().put(FilaRedisKeys.configuracion(configuracion.eventoId()), FilaRedisFields.TAMANIO_TANDA,
                Integer.toString(configuracion.tamanioTanda()));
        redis.opsForHash().put(FilaRedisKeys.configuracion(configuracion.eventoId()), FilaRedisFields.VENTANA_COMPRA_SEGUNDOS,
                Long.toString(configuracion.ventanaCompra().toSeconds()));
    }

    public void actualizarEstado(Long eventoId, EstadoEventoFila estado) {
        redis.opsForHash().put(FilaRedisKeys.configuracion(eventoId), FilaRedisFields.ESTADO, estado.name());
    }

    private static Long numeroOpcional(Object valor) {
        return valor == null ? null : Long.parseLong(texto(valor));
    }

    private static String texto(Object valor) {
        return String.valueOf(valor);
    }

    public record ResultadoIngreso(boolean reingreso, String ticketId, String estado, long ingresadoEnEpochMillis) {
        static ResultadoIngreso eventoNoHabilitado() {
            return new ResultadoIngreso(false, null, null, 0);
        }

        public boolean eventoNoHabilitado() {
            return ticketId == null;
        }
    }

    public record TicketAlmacenado(String ticketId, String estado, long ingresadoEnEpochMillis,
                                   Long habilitadoHastaEpochMillis) {
    }
}
