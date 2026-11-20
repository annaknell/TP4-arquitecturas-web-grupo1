# Entrega 2 — Fila Virtual, Notificaciones y Reportes

## Responsabilidades y datos propios

| Servicio | Responsabilidad | Persistencia | API / eventos |
| --- | --- | --- | --- |
| Fila Virtual | Ordenar ingreso a ventas de alta demanda, tandas y autorización temporal | Redis: cola ordenada por timestamp del servidor, tickets y vencimientos | `/api/fila/**`; recibe apertura/suspensión/cierre desde Catálogo; Inventario valida autorizaciones |
| Notificaciones | Envío asíncrono simulado por email; no revierte operaciones de negocio | BD propia: envío, destinatario, tipo, estado, intentos y correlación; RabbitMQ | Consume compra, publicación, modificación y cancelación; persiste PENDIENTE, ENVIADO, FALLIDO o REINTENTO |
| Reportes | Modelo de lectura por evento y sector | BD propia: evento, sector, métricas y eventos procesados; Redis como caché | `/api/reportes/**`; consume configuración, ventas y reintegros |

## Criterios de aceptación acordados

### Fila virtual

- Un cliente entra una sola vez por evento; al reingresar recupera el ticket, posición y estado.
- La posición se calcula con orden por timestamp asignado por el servidor Redis, no por reloj del cliente.
- Catálogo configura tandas y una ventana de compra de 10 minutos. Al vencer, la autorización queda inutilizable.
- Un evento normal no exige fila. En alta demanda, Inventario rechaza una reserva sin ticket habilitado.
- Catálogo puede pausar, reabrir o cancelar la fila; agotamiento cierra nuevas habilitaciones.
- Errores: evento inexistente `404`, ticket de otro cliente/expirado `403`, estado no habilitable `409`.

### Notificaciones

- Canal inicial: email simulado.
- Compra confirmada, publicación/favorito, modificación y cancelación generan mensajes asíncronos.
- Una falla de email no revierte la compra ni el cambio del evento.
- Se reintenta con demora creciente hasta el máximo configurado; luego queda `FALLIDO`.
- Idempotencia: una cancelación por comprador/evento y una recomendación por cliente/evento.
- Cada envío guarda `eventId`, `correlationId`, intento y estado.

### Reportes — US-41, US-42 y US-43

- US-41: muestra vendidas y devueltas por evento y sector. Sólo cuenta ventas confirmadas y reintegros aplicados.
- US-42: `entradasValidas / capacidad`; válidas = vendidas − devueltas. Capacidad numerada = asientos; general = cupo máximo.
- US-43: bruto = ventas confirmadas; reintegrado = reintegros aprobados; neto = bruto − reintegrado; pendiente = `PENDIENTE` o `INCIERTO`, separado del neto.
- Los reportes permiten consistencia eventual de segundos. `actualizadoEn` informa la última proyección aplicada.
- Organizador consulta sólo eventos cuyo `organizadorId` coincide con JWT `userId`; administrador consulta cualquiera; otros roles `403`; token inválido/ausente `401`.

## Contrato asíncrono común

Todo evento usa `eventId` UUID, `eventType`, `occurredAt`, `correlationId`, `source` y `payload`. La entrega es al menos una vez: consumidores deduplican por `eventId`.

| Evento | Productor | Consumidor | Datos relevantes |
| --- | --- | --- | --- |
| `COMPRA_CONFIRMADA` / `VENTA_CONFIRMADA` | Compras | Notificaciones, Reportes | compra, evento, líneas entrada/sector/importe |
| `EVENTO_CONFIGURADO` / `EVENTO_PUBLICADO` | Catálogo | Reportes, Notificaciones, Fila | evento, organizador, capacidad, sectores, alta demanda |
| `EVENTO_MODIFICADO` / `EVENTO_CANCELADO` | Catálogo | Notificaciones, Fila, Pagos | evento, cambios o motivo |
| `REINTEGRO_REGISTRADO` / `REINTEGRO_APLICADO` | Pagos | Reportes | entrada, evento, sector, importe y estado |
| `ENTRADA_DEVUELTA` | Entradas | Reportes | entrada, evento, sector, causa |

## Endpoints, permisos y pruebas

- Fila: `/api/fila/**`; cliente identificado consulta/entra, endpoints internos bajo `/api/interno/fila/**`.
- Notificaciones: `/api/notificaciones/**`; administración restringida a administrador.
- Reportes: `GET /api/reportes/eventos`, `GET /api/reportes/eventos/{id}`, `GET /api/reportes/eventos/{id}/sectores`.
- Colección Postman debe cubrir JWT faltante/inválido, organizador propio/ajeno, administrador, turno vencido, duplicados y reintentos.
- Integración mínima: compra aumenta bruto; reintegro pendiente/aprobado mueve importes; configuración crea proyección; mensaje duplicado no altera totales; fallo de consumidor termina en DLQ.

## Operación

RabbitMQ transporta eventos; `reportes.proyecciones` usa reintentos y DLQ. Redis es fuente operativa de Fila y caché descartable de Reportes. Cada servicio conserva su propia base. Docker Compose debe incluir RabbitMQ, Redis, MySQL de los tres servicios y sus variables de conexión.
