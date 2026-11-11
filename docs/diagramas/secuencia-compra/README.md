# Diagramas de secuencia: compra de una entrada numerada

Saga de compra de la Entrega 1: el cliente pide entradas numeradas y `compra-service` orquesta
a los demás servicios. Un diagrama por caso.

Los nombres y los puertos son los de los servicios del repositorio: compra (8084), inventario
(8083), pago (8085) y entrada (8086). Los endpoints y los contratos son los de cada servicio
(controladores de inventario, pagos, reintegros y entradas).

## 1. Camino normal

El cliente pide la compra, inventario retiene los asientos por 10 minutos, pago cobra y
inventario confirma la reserva; recién entonces se emiten las entradas.

```mermaid
sequenceDiagram
    actor C as Cliente
    participant CO as Compra 8084
    participant IN as Inventario 8083
    participant PA as Pago 8085
    participant EN as Entrada 8086

    C->>CO: POST /api/compras (vía API Gateway)<br/>Idempotency-Key + X-User-Id<br/>{eventoId, asientos[], precioTotal}
    CO->>CO: findByIdempotencyKey(clave)<br/>si ya existe, devuelve esa compra
    CO->>CO: countByClienteIdAndEventoIdAndEstado()<br/>compras previas + asientos <= 4
    Note right of CO: guarda la compra PENDIENTE<br/>paso "Inicio de compra"
    CO->>IN: POST /api/inventario/reservas<br/>{eventoId, asientos[]}
    IN->>IN: retiene los asientos<br/>reserva ACTIVA, expiraEn = ahora + 10 min
    IN-->>CO: 201 {id, estado, expiraEn}
    Note right of CO: guarda reservaId<br/>paso "Asientos reservados"
    CO->>PA: POST /api/pagos/cobrar<br/>Idempotency-Key<br/>{compraId, clienteId, monto, moneda}
    Note right of PA: cobra en la pasarela<br/>idempotente por clave
    PA-->>CO: 201 {id, estado}
    Note right of CO: guarda pagoId<br/>paso "Pago aprobado"
    CO->>IN: POST /api/inventario/reservas/{id}/confirmar
    IN->>IN: RETENIDO pasa a VENDIDO<br/>reserva CONFIRMADA
    IN-->>CO: 200 {estado = CONFIRMADA}
    CO->>EN: POST /api/entradas/emitir<br/>una EntradaRequest por asiento
    EN-->>CO: entradas emitidas
    CO->>CO: estado COMPLETADA<br/>paso "Entradas emitidas con éxito"
    CO-->>C: 201 Created
```

## 2. Pago rechazado

Si el cobro no se aprueba, `compra-service` compensa liberando la reserva y responde 402. Los
asientos vuelven a estar disponibles y nunca se emiten entradas.

```mermaid
sequenceDiagram
    actor C as Cliente
    participant CO as Compra 8084
    participant IN as Inventario 8083
    participant PA as Pago 8085

    C->>CO: POST /api/compras
    CO->>IN: POST /api/inventario/reservas<br/>{eventoId, asientos[]}
    IN->>IN: retiene los asientos<br/>ACTIVA, expiraEn = ahora + 10 min
    IN-->>CO: 201 {id, estado}
    Note right of CO: guarda reservaId<br/>paso "Asientos reservados"
    CO->>PA: POST /api/pagos/cobrar<br/>Idempotency-Key<br/>{compraId, clienteId, monto, moneda}
    PA-->>CO: 201 {estado = RECHAZADO}
    Note over CO,IN: compensación
    CO->>IN: POST /api/inventario/reservas/{id}/liberar
    IN->>IN: libera los asientos retenidos
    IN-->>CO: 200 reserva LIBERADA
    CO->>CO: estado PAGO_RECHAZADO<br/>paso "Pago rechazado - Liberando reserva"
    CO-->>C: 402 Payment Required
```

## 3. Reserva vencida

El cobro se aprueba después de los 10 minutos. El barrido programado de inventario ya venció la
reserva y liberó los lugares, así que al confirmar responde 409 y compra pide el reintegro.

```mermaid
sequenceDiagram
    actor C as Cliente
    participant CO as Compra 8084
    participant IN as Inventario 8083
    participant PA as Pago 8085

    CO->>IN: POST /api/inventario/reservas<br/>{eventoId, asientos[]}
    IN->>IN: reserva ACTIVA<br/>expiraEn = ahora + inventario.reserva.minutos (10)
    IN-->>CO: 201 {id, estado, expiraEn}
    Note over CO,PA: pasan los 10 minutos sin que llegue el cobro
    IN->>IN: @Scheduled cada 5000 ms: barrerVencidas()<br/>liberarRetencionesVencidas(ahora)<br/>findByEstadoAndExpiraEnBefore(ACTIVA, ahora)<br/>vencerSiActiva(...) -> reserva VENCIDA
    Note over IN: los asientos vuelven a estar libres
    Note over CO,PA: el cobro se aprueba después del vencimiento
    CO->>PA: POST /api/pagos/cobrar<br/>Idempotency-Key<br/>{compraId, clienteId, monto, moneda}
    PA-->>CO: 201 {estado = APROBADO}
    CO->>IN: POST /api/inventario/reservas/{id}/confirmar
    IN-->>CO: 409 Conflict "La reserva venció a las ..."
    CO->>PA: POST /api/reintegros/compra/{compraId}<br/>{motivo}
    PA-->>CO: 201 Reintegro
    CO->>CO: estado RESERVA_VENCIDA<br/>paso "Reserva vencida - Reintegro solicitado"
    CO-->>C: 409 Conflict
```

## Archivos

| Archivo | Qué contiene |
|---|---|
| `1-compra-camino-normal.puml` / `.png` / `.svg` | Fuente PlantUML y las dos imágenes del camino normal |
| `2-compra-pago-rechazado.puml` / `.png` / `.svg` | Fuente y imágenes de la compensación por pago rechazado |
| `3-compra-reserva-vencida.puml` / `.png` / `.svg` | Fuente y imágenes de la compensación por reserva vencida |

Las tres figuras también están en `TP4 - Secuencia compra entrada numerada.pdf`.

## Cómo se regeneran

Las fuentes son los `.puml`. Se renderizan con el servidor público de PlantUML (Kroki), sin
instalar nada:

```bash
curl -s -X POST -H "Content-Type: text/plain" \
     --data-binary @1-compra-camino-normal.puml \
     https://kroki.io/plantuml/svg -o 1-compra-camino-normal.svg
```

Para editar a mano, el mismo contenido se pega en plantuml.com o planttext.com.
