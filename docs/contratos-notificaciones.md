# Contratos de integración — Notificaciones

Este documento define los acuerdos de integración entre `notificaciones-service` y los demás
microservicios antes de implementar código. Los contratos permiten que los servicios evolucionen
de forma independiente, siempre que mantengan estos datos y reglas.

## Objetivo

Notificaciones recibe hechos de negocio ya confirmados, registra el trabajo de envío y lo procesa
de forma asíncrona. Una falla al enviar un email o una notificación *push* no debe revertir ni
bloquear una compra o un cambio de evento.

## Transporte propuesto

El transporte recomendado es una cola durable mediante RabbitMQ:

```text
compra-service / catalogo-service --> RabbitMQ --> notificaciones-service
```

La cola desacopla al productor del consumidor: si `notificaciones-service` está temporalmente
caído, el mensaje permanece disponible para procesarlo luego. El nombre definitivo de los
*exchanges*, colas y rutas se acordará al incorporar RabbitMQ.

## Sobre común de eventos

Todos los eventos destinados a Notificaciones deben incluir el siguiente sobre:

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| `eventId` | UUID | Sí | Identificador único e inmutable del evento. Permite detectar mensajes repetidos. |
| `eventType` | texto | Sí | Tipo de evento de negocio. |
| `occurredAt` | fecha ISO-8601 UTC | Sí | Momento en que ocurrió el hecho de negocio. |
| `correlationId` | texto/UUID | Sí | Mismo valor que `X-Correlation-Id`; permite seguir la operación entre servicios. |
| `source` | texto | Sí | Servicio que publicó el evento. |
| `payload` | objeto | Sí | Datos específicos del evento. |

Ejemplo general:

```json
{
  "eventId": "a15d9f18-5cda-4a02-a81d-3b806f273db2",
  "eventType": "COMPRA_COMPLETADA",
  "occurredAt": "2026-11-18T17:30:00Z",
  "correlationId": "6e36428f-1690-4605-8c40-788bdd5f0c88",
  "source": "compra-service",
  "payload": {}
}
```

## Eventos

### `COMPRA_COMPLETADA`

- **Publica:** `compra-service`.
- **Consume:** `notificaciones-service`.
- **Condición de publicación:** el pago fue confirmado, la reserva fue confirmada y las entradas
  fueron emitidas.
- **Efecto esperado:** crear una confirmación de compra para el cliente.

```json
{
  "eventId": "uuid",
  "eventType": "COMPRA_COMPLETADA",
  "occurredAt": "2026-11-18T17:30:00Z",
  "correlationId": "uuid",
  "source": "compra-service",
  "payload": {
    "compraId": 123,
    "clienteId": 45,
    "email": "cliente@example.com",
    "eventoId": 10,
    "nombreEvento": "Recital",
    "fechaEvento": "2026-12-20T21:00:00Z",
    "lugar": "Teatro X",
    "entradas": [
      {
        "entradaId": 800,
        "sector": "Plateas",
        "asiento": "A-12",
        "codigoQr": "valor-del-qr"
      }
    ]
  }
}
```

### `EVENTO_CANCELADO`

- **Publica:** `catalogo-service`.
- **Consume:** `notificaciones-service`.
- **Condición de publicación:** la cancelación del evento se confirmó correctamente.
- **Efecto esperado:** notificar una sola vez a cada comprador afectado.

```json
{
  "eventId": "uuid",
  "eventType": "EVENTO_CANCELADO",
  "occurredAt": "2026-11-18T17:30:00Z",
  "correlationId": "uuid",
  "source": "catalogo-service",
  "payload": {
    "eventoId": 10,
    "nombreEvento": "Recital",
    "motivo": "Cancelación por condiciones climáticas"
  }
}
```

> Pendiente de acordar: cómo Notificaciones obtiene los destinatarios de una cancelación. La
> opción recomendada es exponer una consulta interna de `compra-service` o `entrada-service` que
> devuelva compradores únicos y sus medios de contacto para un `eventoId`.

### `EVENTO_MODIFICADO`

- **Publica:** `catalogo-service`.
- **Consume:** `notificaciones-service`.
- **Condición de publicación:** cambió la fecha, hora o lugar de un evento ya publicado.
- **Efecto esperado:** notificar una sola vez a cada comprador afectado, describiendo los cambios.

```json
{
  "eventId": "uuid",
  "eventType": "EVENTO_MODIFICADO",
  "occurredAt": "2026-11-18T17:30:00Z",
  "correlationId": "uuid",
  "source": "catalogo-service",
  "payload": {
    "eventoId": 10,
    "nombreEvento": "Recital",
    "cambios": {
      "fechaAnterior": "2026-12-20T21:00:00Z",
      "fechaNueva": "2026-12-22T21:00:00Z",
      "lugarAnterior": "Teatro X",
      "lugarNuevo": "Estadio Y"
    }
  }
}
```

## Reglas de procesamiento

1. Notificaciones registra primero el evento y luego realiza el envío de manera asíncrona.
2. Un mismo `eventId` no puede procesarse dos veces.
3. Para un mismo evento, destinatario y canal debe existir como máximo una notificación.
4. Cada log y cada comunicación posterior debe conservar el `correlationId` recibido.
5. Un fallo del proveedor de email o *push* no se comunica como error al servicio productor.
6. Los envíos fallidos se reintentan según la política acordada; si se agotan los intentos quedan
   disponibles para reproceso manual.

## Información pendiente de acordar

- Canal inicial: email simulado, email real o notificación *push*.
- Fuente de los datos de contacto de los clientes.
- Consulta interna para recuperar compradores de un evento cancelado o modificado.
- Cantidad de reintentos, tiempo de espera y estado final de una notificación fallida.
- Nombres definitivos de las colas y estrategia de mensajes no procesables (DLQ).
