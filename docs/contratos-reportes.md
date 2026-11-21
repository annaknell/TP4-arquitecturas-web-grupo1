# Contratos de eventos para reportes

Los hechos publicados para reportes se envían a `reportes.exchange` (tipo
`topic`, durable). El sobre es versionado y contiene siempre `eventId`,
`eventType`, `schemaVersion`, `occurredAt`, `correlationId`, `source` y
`payload`. Los importes se expresan como enteros en centavos y con moneda ISO
4217.

## Eventos

| Tipo | Routing key | Productor | Payload obligatorio |
| --- | --- | --- | --- |
| `EVENTO_CONFIGURADO` | `catalogo.evento.configurado` | catálogo | `eventoId`, `organizadorId`, sectores con `sectorId`, tipo y capacidad |
| `VENTA_CONFIRMADA` | `ventas.confirmada` | compras | `compraId`, `eventoId`, moneda y líneas con `entradaId`, `sectorId`, `importeCentavos` |
| `REINTEGRO_REGISTRADO` | `reintegros.registrado` | pagos | `reintegroId`, estado y asignaciones por entrada/sector |
| `REINTEGRO_APLICADO` | `reintegros.aplicado` | pagos | el reintegro aprobado y sus asignaciones |
| `ENTRADA_INVALIDADA` | `entradas.invalidada` | entradas | `entradaId`, `eventoId`, `sectorId` y causa |

## Entrega y tolerancia a fallos

Cada productor debe escribir el hecho en una outbox dentro de la misma
transacción que modifica su dato de negocio. Un publicador reintentable remite
la outbox al broker; las colas de consumidores usan reintentos con backoff y
DLQ. La entrega es al menos una vez: el consumidor deduplica por `eventId` en
su inbox antes de alterar una proyección.

El orden se considera por entidad (`compraId`, `reintegroId` o `eventoId`), no
global. Un cambio incompatible del payload requiere aumentar `schemaVersion`.
