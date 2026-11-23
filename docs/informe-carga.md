# Informe de la prueba de carga

Se probó el flujo de compra completo (registro, login, fila virtual y `POST /api/compras`, que reserva el
asiento, cobra, confirma y emite la entrada con QR) contra el sistema levantado con `docker-compose`, con
20, 50 y 100 clientes concurrentes. Cada cliente compra su propia butaca de un sector de 100 lugares.

Herramienta: Apache JMeter 5.6.3, plan `loadtest/ticketya-compra.jmx`, con arranque simultáneo de todos los
clientes y una compra por cliente. Entorno: un solo equipo, 8 GB de RAM, los 11 servicios y las bases en
contenedores.

## Resultados

| Usuarios concurrentes | Compras exitosas | Errores | p50 | p95 | p99 | Throughput |
| --- | --- | --- | --- | --- | --- | --- |
| 20 | 18 / 20 | 10 % | 0,67 s | 1,46 s | 1,47 s | 11,1 compras/s |
| 50 | 49 / 50 | 2 % | 3,02 s | 3,78 s | 3,79 s | 10,4 compras/s |
| 100 | 97 / 100 | 3 % | 2,48 s | 3,61 s | 3,64 s | 16,1 compras/s |

## Errores

- 409 `PAGO_INCIERTO` (2 de 20, 1 de 50, 2 de 100): el cobro supera el timeout de 3 segundos entre
  servicios y la compra queda en `PAGO_INCIERTO` para reconciliación.
- 500 (1 de 100): al fallar un paso, la compra no puede guardar el detalle porque el texto supera los 255
  caracteres de `paso_compra.descripcion`.
- Ninguna butaca se vendió dos veces en ninguna de las tres corridas.

## Conclusión

El flujo de compra funciona completo bajo carga y el inventario se mantiene consistente. La capacidad
medida es de 10 a 16 compras por segundo, con p95 de 3,6 s a 100 clientes concurrentes; no crece al sumar
usuarios. El costo está en las cuatro llamadas síncronas de cada compra y en el registro/login, no en la
fila virtual, que responde en milisegundos.

## Cómo reproducirlo

```bash
jmeter -n -t loadtest/ticketya-compra.jmx -JhilosIngreso=100 -JhilosCompra=0 -Jrun=r4 -JeventoId=3 -JasientoBase=201
docker exec ticketya-api-gateway-service-1 curl -s -X POST http://fila-virtual-service:8086/api/interno/fila/3/tandas/habilitar
jmeter -n -t loadtest/ticketya-compra.jmx -JhilosIngreso=0 -JhilosCompra=100 -Jrun=r4 -JeventoId=3 -JasientoBase=201
```

## Notas posteriores a la auditoría

Los resultados anteriores corresponden a la corrida original; después de los arreglos de la Entrega 2 no se
repitió la prueba de carga. Los puertos vigentes usados por el plan son `fila-virtual-service:8086` y
`entrada-service:8090`. El rechazo por límite de entradas por cliente se responde ahora con `422` antes de
cobrar, y los fallos de emisión con reserva confirmada reintegran y dejan la compra en `REINTEGRADA` en vez
de `FALLO_SISTEMA`.
