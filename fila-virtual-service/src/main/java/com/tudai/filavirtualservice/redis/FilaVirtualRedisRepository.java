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

    private static final DefaultRedisScript<String> VALIDAR_Y_CONSUMIR_AUTORIZACION_SCRIPT = new DefaultRedisScript<>("""
            local estadoFila = redis.call('HGET', KEYS[1], 'estado')
            if not estadoFila then return 'NO_REQUIERE_FILA' end
            if estadoFila ~= 'ABIERTA' then return 'FILA_NO_ABIERTA' end
            local ahora = redis.call('TIME')
            local ahoraMillis = ahora[1] * 1000 + math.floor(ahora[2] / 1000)
            if redis.call('HGET', KEYS[2], 'eventoId') ~= ARGV[1] then return 'TOKEN_INVALIDO' end
            if redis.call('HGET', KEYS[2], 'clienteId') ~= ARGV[2] then return 'TOKEN_INVALIDO' end
            local venceEn = tonumber(redis.call('HGET', KEYS[2], 'venceEn'))
            if not venceEn or venceEn <= ahoraMillis then return 'TOKEN_VENCIDO' end
            if redis.call('HGET', KEYS[3], 'estado') ~= 'HABILITADO' then return 'TOKEN_NO_HABILITADO' end
            if redis.call('HGET', KEYS[3], 'autorizacionToken') ~= ARGV[3] then return 'TOKEN_INVALIDO' end
            redis.call('HSET', KEYS[3], 'estado', 'CONSUMIDO')
            redis.call('HDEL', KEYS[3], 'autorizacionToken')
            redis.call('DEL', KEYS[2])
            redis.call('ZREM', KEYS[4], ARGV[1] .. ':' .. ARGV[2] .. ':' .. ARGV[3])
            return 'CONSUMIDA'
            """, String.class);

    private static final DefaultRedisScript<String> RESTAURAR_AUTORIZACION_SCRIPT = new DefaultRedisScript<>("""
            local ahora = redis.call('TIME')
            local ahoraMillis = ahora[1] * 1000 + math.floor(ahora[2] / 1000)
            if redis.call('HGET', KEYS[1], 'estado') ~= 'CONSUMIDO' then return 'TOKEN_NO_CONSUMIDO' end
            local venceEn = tonumber(redis.call('HGET', KEYS[1], 'habilitadoHasta'))
            if not venceEn or venceEn <= ahoraMillis then return 'TOKEN_VENCIDO' end
            local ticketId = redis.call('HGET', KEYS[1], 'ticketId')
            local ttlSegundos = math.ceil((venceEn - ahoraMillis) / 1000)
            redis.call('HSET', KEYS[1], 'estado', 'HABILITADO', 'autorizacionToken', ARGV[3])
            redis.call('HSET', KEYS[2],
                'eventoId', ARGV[1],
                'clienteId', ARGV[2],
                'ticketId', ticketId,
                'venceEn', venceEn)
            redis.call('EXPIRE', KEYS[2], ttlSegundos)
            redis.call('ZADD', KEYS[3], venceEn, ARGV[1] .. ':' .. ARGV[2] .. ':' .. ARGV[3])
            return 'RESTAURADA'
            """, String.class);

    private static final DefaultRedisScript<List> HABILITAR_TANDA_SCRIPT = new DefaultRedisScript<>("""
            local estadoEvento = redis.call('HGET', KEYS[1], 'estado')
            if estadoEvento ~= 'ABIERTA' then
                return {'EVENTO_NO_HABILITADO'}
            end
            local ventanaSegundos = tonumber(redis.call('HGET', KEYS[1], 'ventanaCompraSegundos'))
            local clientes = redis.call('ZRANGE', KEYS[2], 0, tonumber(ARGV[1]) - 1)
            if #clientes == 0 then
                return {'SIN_PERSONAS'}
            end
            local ahora = redis.call('TIME')
            local venceEn = ahora[1] * 1000 + math.floor(ahora[2] / 1000) + ventanaSegundos * 1000
            local respuesta = {'HABILITADOS'}
            for indice, clienteId in ipairs(clientes) do
                local token = ARGV[indice + 1]
                local ticketKey = KEYS[3] .. clienteId
                local ticketId = redis.call('HGET', ticketKey, 'ticketId')
                redis.call('HSET', ticketKey,
                    'estado', 'HABILITADO',
                    'habilitadoHasta', venceEn,
                    'autorizacionToken', token)
                local autorizacionKey = KEYS[4] .. token
                redis.call('HSET', autorizacionKey,
                    'eventoId', ARGV[#ARGV],
                    'clienteId', clienteId,
                    'ticketId', ticketId,
                    'venceEn', venceEn)
                redis.call('EXPIRE', autorizacionKey, ventanaSegundos)
                local vencimientoId = ARGV[#ARGV] .. ':' .. clienteId .. ':' .. token
                redis.call('ZADD', KEYS[5], venceEn, vencimientoId)
                redis.call('ZREM', KEYS[2], clienteId)
                table.insert(respuesta, clienteId)
                table.insert(respuesta, ticketId)
                table.insert(respuesta, token)
                table.insert(respuesta, tostring(venceEn))
            end
            return respuesta
            """, List.class);

    /** Vence hasta cien autorizaciones por ejecución sin modificar tickets con una autorización más reciente. */
    private static final DefaultRedisScript<Long> VENCER_AUTORIZACIONES_SCRIPT = new DefaultRedisScript<>("""
            local ahora = redis.call('TIME')
            local ahoraMillis = ahora[1] * 1000 + math.floor(ahora[2] / 1000)
            local vencimientos = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ahoraMillis, 'LIMIT', 0, 100)
            local cantidadVencida = 0
            for _, vencimientoId in ipairs(vencimientos) do
                local eventoId, clienteId, token = string.match(vencimientoId, '^([^:]+):([^:]+):(.+)$')
                if eventoId and clienteId and token then
                    local ticketKey = KEYS[3] .. eventoId .. ':ticket:' .. clienteId
                    local estado = redis.call('HGET', ticketKey, 'estado')
                    local tokenActual = redis.call('HGET', ticketKey, 'autorizacionToken')
                    if estado == 'HABILITADO' and tokenActual == token then
                        redis.call('HSET', ticketKey, 'estado', 'VENCIDO')
                        redis.call('HDEL', ticketKey, 'autorizacionToken')
                        cantidadVencida = cantidadVencida + 1
                    end
                    redis.call('DEL', KEYS[2] .. token)
                end
                redis.call('ZREM', KEYS[1], vencimientoId)
            end
            return cantidadVencida
            """, Long.class);

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
            return ResultadoIngreso.deEventoNoHabilitado();
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
                numeroOpcional(datos.get(FilaRedisFields.HABILITADO_HASTA)),
                datos.containsKey(FilaRedisFields.AUTORIZACION_TOKEN)
                        ? texto(datos.get(FilaRedisFields.AUTORIZACION_TOKEN)) : null));
    }

    public Long obtenerRango(Long eventoId, Long clienteId) {
        return redis.opsForZSet().rank(FilaRedisKeys.cola(eventoId), clienteId.toString());
    }

    public int vencerAutorizacionesPendientes() {
        Long cantidad = redis.execute(
                VENCER_AUTORIZACIONES_SCRIPT,
                List.of(
                        FilaRedisKeys.vencimientosAutorizacion(),
                        FilaRedisKeys.prefijoAutorizacion(),
                        FilaRedisKeys.prefijoEventos()
                )
        );

        return cantidad == null ? 0 : cantidad.intValue();
    }

    public String validarYConsumirAutorizacion(Long eventoId, Long clienteId, String token) {
        if (token == null || token.isBlank()) {
            return "TOKEN_REQUERIDO";
        }
        String resultado = redis.execute(VALIDAR_Y_CONSUMIR_AUTORIZACION_SCRIPT,
                List.of(FilaRedisKeys.configuracion(eventoId), FilaRedisKeys.autorizacion(token),
                        FilaRedisKeys.ticket(eventoId, clienteId), FilaRedisKeys.vencimientosAutorizacion()),
                eventoId.toString(), clienteId.toString(), token);
        return resultado == null ? "TOKEN_INVALIDO" : resultado;
    }

    public String restaurarAutorizacion(Long eventoId, Long clienteId, String token) {
        if (token == null || token.isBlank()) {
            return "TOKEN_REQUERIDO";
        }
        String resultado = redis.execute(RESTAURAR_AUTORIZACION_SCRIPT,
                List.of(FilaRedisKeys.ticket(eventoId, clienteId), FilaRedisKeys.autorizacion(token),
                        FilaRedisKeys.vencimientosAutorizacion()),
                eventoId.toString(), clienteId.toString(), token);
        return resultado == null ? "TOKEN_INVALIDO" : resultado;
    }

    public ResultadoTanda habilitarSiguienteTanda(Long eventoId, int tamanioTanda, List<String> tokens) {
        java.util.ArrayList<String> argumentos = new java.util.ArrayList<>();
        argumentos.add(Integer.toString(tamanioTanda));
        argumentos.addAll(tokens);
        argumentos.add(eventoId.toString());
        List resultado = redis.execute(HABILITAR_TANDA_SCRIPT,
                List.of(FilaRedisKeys.configuracion(eventoId), FilaRedisKeys.cola(eventoId),
                        FilaRedisKeys.prefijoTicket(eventoId), FilaRedisKeys.prefijoAutorizacion(),
                        FilaRedisKeys.vencimientosAutorizacion()), argumentos.toArray());
        if (resultado == null || resultado.isEmpty()) {
            throw new IllegalStateException("Redis no devolvió resultado al habilitar la tanda");
        }
        if ("EVENTO_NO_HABILITADO".equals(texto(resultado.getFirst()))) {
            return ResultadoTanda.deEventoNoHabilitado();
        }
        if ("SIN_PERSONAS".equals(texto(resultado.getFirst()))) {
            return new ResultadoTanda(false, List.of());
        }
        java.util.ArrayList<HabilitacionCreada> habilitaciones = new java.util.ArrayList<>();
        for (int indice = 1; indice < resultado.size(); indice += 4) {
            habilitaciones.add(new HabilitacionCreada(Long.parseLong(texto(resultado.get(indice))),
                    texto(resultado.get(indice + 1)), texto(resultado.get(indice + 2)),
                    Long.parseLong(texto(resultado.get(indice + 3)))));
        }
        return new ResultadoTanda(false, List.copyOf(habilitaciones));
    }

    public int obtenerTamanioTanda(Long eventoId) {
        Object valor = redis.opsForHash().get(FilaRedisKeys.configuracion(eventoId), FilaRedisFields.TAMANIO_TANDA);
        return valor == null ? 0 : Integer.parseInt(texto(valor));
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
        static ResultadoIngreso deEventoNoHabilitado() {
            return new ResultadoIngreso(false, null, null, 0);
        }

        public boolean eventoNoHabilitado() {
            return ticketId == null;
        }
    }

    public record TicketAlmacenado(String ticketId, String estado, long ingresadoEnEpochMillis,
                                   Long habilitadoHastaEpochMillis, String autorizacionToken) {
    }

    public record ResultadoTanda(boolean eventoNoHabilitado, List<HabilitacionCreada> habilitaciones) {
        static ResultadoTanda deEventoNoHabilitado() {
            return new ResultadoTanda(true, List.of());
        }
    }

    public record HabilitacionCreada(Long clienteId, String ticketId, String token, long venceEnEpochMillis) {
    }
}
