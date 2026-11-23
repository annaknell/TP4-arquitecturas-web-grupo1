# Informe técnico — Entrega 2

## 1. Resumen ejecutivo y propósito

**TicketYa** es una plataforma para la gestión y comercialización de entradas para eventos. El código implementa catálogo de eventos y lugares, inventario de localidades, compras, cobros y reintegros, emisión de entradas, fila virtual para eventos de alta demanda, autenticación, notificaciones, preferencias y reportes.

El problema de negocio abordado es la venta de tickets bajo concurrencia, evitando la sobreventa mediante reservas temporales, confirmación de inventario e idempotencia en compras y pagos. También se implementan cancelaciones, reintegros, avisos a compradores y proyecciones de ventas.

No existe una interfaz web o móvil en el repositorio. La interfaz expuesta es una colección de APIs REST, documentables mediante OpenAPI/Swagger; además hay una colección Postman y documentación de contratos y diagramas en `docs/`.

## 2. Stack tecnológico y dependencias

- **Lenguaje y compilación:** Java 21 y Maven.
- **Framework principal:** Spring Boot. La mayoría de los servicios usan la versión `4.1.2`; `pago-service` usa `4.0.0`.
- **HTTP:** Spring MVC / Spring Web.
- **Gateway:** Spring Cloud Gateway MVC con Spring Cloud `2025.1.4`.
- **Comunicación síncrona:** OpenFeign, usado por `compra-service`.
- **Persistencia relacional:** Spring Data JPA y MySQL.
- **Persistencia documental:** MongoDB en `recomendaciones-service`.
- **Caché y estado temporal:** Redis en fila virtual y caché de reportes.
- **Mensajería:** RabbitMQ con Spring AMQP.
- **Seguridad:** Spring Security, JWT RS256 y publicación de claves JWK.
- **Documentación:** springdoc OpenAPI, en versiones `2.5.0`, `2.8.6`, `3.0.0` y `3.2.0` según el módulo.
- **Otros:** Lombok, Jakarta Validation y Spring Boot Actuator en inventario y pagos.
- **Pruebas:** Spring Test y H2 en módulos concretos.

Los servicios incluyen Maven Wrapper individual. El `pom.xml` raíz agrega los once módulos del repositorio, incluidos `auth-service` y `recomendaciones-service`; por ello, un build Maven ejecutado desde la raíz sí los incorpora. El proyecto raíz no incluye Maven Wrapper propio.

No se encontraron Docker Compose, configuración de CI/CD, infraestructura como código ni frontend. Solo `auth-service` contiene un `Dockerfile`.

## 3. Arquitectura y estructura del proyecto

La solución sigue una arquitectura de **microservicios**, con bases de datos por contexto y comunicaciones REST síncronas, eventos RabbitMQ y Redis.

| Servicio | Responsabilidad | Puerto |
|---|---|---:|
| `api-gateway-service` | Enrutamiento HTTP y Swagger agregado | 8080 |
| `auth-service` | Usuarios, roles, login, JWT y JWK | 8081 |
| `catalogo-service` | Eventos, lugares, sectores, asientos y participantes | 8082 |
| `inventario-service` | Disponibilidad, reservas y retención de cupos/asientos | 8083 |
| `compra-service` | Orquestación de compra y compensaciones | 8084 |
| `pago-service` | Cobros, pasarela mock y reintegros | 8085 |
| `entrada-service` | Emisión, consulta y cancelación de entradas | 8086 |
| `recomendaciones-service` | Preferencias y recomendaciones | 8087 |
| `notificaciones-service` | Eventos, reintentos y email simulado | 8088 |
| `reportes-service` | Proyecciones de ventas y reintegros | 8089 |
| `fila-virtual-service` | Fila de alta demanda y autorizaciones temporales | 8090 |

### Organización interna

Los módulos siguen una separación por capas:

- `controller`: endpoints REST.
- `service`: reglas de negocio y orquestación.
- `repository`: acceso a persistencia.
- `entity` o `domain`: modelos persistentes y objetos de dominio.
- `dto`: contratos de entrada y salida.
- `client`: integraciones HTTP entre servicios.
- `messaging`: publicadores y consumidores RabbitMQ.
- `config`, `security`, `scheduler` y `exception`: preocupaciones transversales.

### Integraciones implementadas

- `compra-service` invoca por Feign a catálogo, inventario, pagos y entradas.
- Catálogo publica eventos hacia notificaciones y reportes.
- Compras publica ventas confirmadas para reportes y eventos de compra completada para notificaciones.
- Pagos publica eventos de reintegro hacia reportes.
- Reportes y notificaciones consumen eventos RabbitMQ.
- Catálogo e inventario integran con fila virtual mediante clientes HTTP.
- Fila virtual mantiene configuraciones, posiciones y tickets en Redis.

### API REST implementada

El código expone operaciones de autenticación, alta de usuarios administrativa, catálogo de eventos y lugares, disponibilidad y reservas, creación y consulta de compras, cobros y reintegros, emisión y cancelación de entradas, fila virtual, notificaciones, preferencias, recomendaciones y reportes.

Entre los endpoints relevantes se encuentran:

- `/api/auth`: registro, login, usuario actual, alta de usuarios y JWK.
- `/api/eventos` y `/api/lugares`: catálogo y ciclo de vida de eventos.
- `/api/inventario`: disponibilidad, reservas, confirmación, liberación y aprovisionamiento.
- `/api/compras`: creación, consulta y consulta interna de compradores.
- `/api/pagos` y `/reintegros`: cobros, pasarela mock y reintegros.
- `/api/entradas`: emisión, consulta y cancelación.
- `/api/fila` y `/api/interno/fila`: ingreso, posición, tandas y autorizaciones.
- `/api/notificaciones`, `/api/preferencias`, `/api/recomendaciones` y `/api/reportes`.

### Observaciones de integración

- El gateway enruta catálogo bajo `/api/eventos/**`, pero no tiene una ruta equivalente para `/api/lugares/**`.
- No se observan rutas del gateway para autenticación ni entradas.
- La URL configurada para documentación de reportes usa `/v3api-docs` en lugar de `/v3/api-docs`.
- El código fuente ubica fila virtual en el puerto 8090, mientras que artefactos previos bajo `target/` refieren al 8086. El código fuente actual es la referencia válida.

## 4. Modelo de datos y persistencia

No se usan migraciones versionadas con Flyway o Liquibase. Los servicios relacionales utilizan `spring.jpa.hibernate.ddl-auto=update`, por lo que el esquema se crea o altera automáticamente durante el inicio.

### MySQL por servicio

Las bases configuradas son `ticket_ya_auth`, `ticket_ya_catalogo`, `ticket_ya_inventario`, `ticket_ya_compras`, `ticket_ya_entradas`, `ticket_ya_pagos`, `ticket_ya_notificaciones` y `ticket_ya_reportes`.

Esto implementa aislamiento de datos entre servicios. Los vínculos con entidades alojadas en otros servicios son referencias por ID, no claves foráneas distribuidas.

### Autenticación

`Usuario` contiene UUID, email único, hash de contraseña, nombre, rol, estado activo y fecha de creación. Los roles definidos son `CLIENTE`, `ORGANIZADOR`, `ADMINISTRADOR` y `PERSONAL_ACCESO`.

### Catálogo

- `Lugar` posee sectores.
- `Sector` pertenece a un lugar y posee asientos.
- `Asiento` pertenece a un sector.
- `Evento` pertenece a un lugar y tiene una relación muchos-a-muchos con participantes.
- `EventoSector` vincula evento y sector, con precio y capacidad disponible.
- `organizadorId` es una referencia lógica a un usuario de autenticación.

### Inventario

- `ConfiguracionEvento`: estado y máximo de entradas por cliente.
- `ZonaCupo`: inventario agregado para sectores sin numeración.
- `EventoAsiento`: estado de un asiento por evento (`LIBRE`, `RETENIDO` o `VENDIDO`), reserva y vencimiento.
- `Reserva`: retención temporal de inventario.

El servicio incluye barrido programado de reservas y reglas de expiración.

### Compras

- `Compra`: cliente, evento, clave de idempotencia única, reserva, pago, total, estado y fecha.
- `PasoCompra`: registro de pasos ejecutados y compensados.
- `LineaVenta`: instantánea de entrada, sector, asiento, importe y moneda para reportes.
- `compra_asientos`: colección de IDs de asientos vinculada a una compra.

### Pagos y reintegros

- `Pago`: compra, cliente, importe, moneda, idempotencia, referencia de pasarela y versión optimista.
- `Reintegro`: pago, compra, motivo, estado, reintentos, solicitud asociada y versión optimista.
- `AsignacionReintegro`: distribución de un reintegro por entrada, evento y sector.

La pasarela es mock y el servicio tiene un scheduler para reintentar reintegros pendientes.

### Entradas

`Entrada` almacena código QR único, estado, evento, sector, asiento opcional, cliente y compra. El modelo contempla `EMITIDA`, `USADA`, `CANCELADA` y `REINTEGRADA`.

### Notificaciones

- `EventoRecibido`: registro del evento consumido.
- `Notificacion`: destinatario, canal, tipo, contenido, estado, reintentos, próximo intento, envío y último error.

Existe reintento planificado y un notificador de email simulado; no se identificó una integración con proveedor real de correo.

### Reportes

- `ReporteEvento`: proyección por evento, organizador, moneda, nombre y sectores.
- `ReporteSector`: capacidad, ventas, devoluciones e importes bruto, reintegrado, pendiente y neto.
- `EventoProcesado`: idempotencia para eventos recibidos.

Reportes se comporta como una proyección de lectura alimentada por eventos, no como una consulta directa a las bases de compra o pagos.

### MongoDB y Redis

`recomendaciones-service` utiliza MongoDB (`ticket_ya_recomendaciones`) con `ClientePreferencias` y `Recomendacion`. Redis respalda el estado de la fila virtual y se configura como caché de reportes con TTL de 30 segundos.

## 5. Funcionalidades implementadas (detalle por módulo)

| Módulo | Casos de uso operativos | Endpoints principales | Archivos de referencia |
|---|---|---|---|
| Gateway | Enrutamiento HTTP y propagación/generación de `X-Correlation-Id`. | Rutas configuradas para eventos, recomendaciones, inventario, compras, pagos, fila, reportes y notificaciones. | `api-gateway-service/src/main/resources/application.yml`, `api-gateway-service/src/main/java/org/example/apigatewayservice/filter/CorrelationIdFilter.java` |
| Autenticación | Registro de clientes, inicio de sesión, emisión JWT RS256, consulta del usuario autenticado, creación administrativa de usuarios y publicación de JWK. | `/api/auth/register`, `/login`, `/me`, `/.well-known/jwks.json`, `/users`. | `auth-service/src/main/java/org/example/authservice/controller/AuthController.java`, `UserController.java`, `service/AuthService.java`, `security/SecurityConfig.java` |
| Catálogo | Alta y listado de lugares; creación, consulta, publicación, modificación, suspensión, reanudación y cancelación de eventos; consulta de sectores/precios. | `/api/lugares`, `/api/eventos`, `/api/eventos/{id}/publicar`, `/cancelar`, `/suspender`, `/reanudar`, `/sectores`. | `catalogo-service/src/main/java/org/example/catalogoservice/controller/EventoController.java`, `LugarController.java`, `service/EventoService.java` |
| Inventario | Disponibilidad, reserva temporal, confirmación, liberación, aprovisionamiento desde catálogo, cambio de estado de evento y liberaciones masivas. Incluye barrido programado de reservas vencidas. | `/api/inventario/eventos/{eventId}/disponibilidad`, `/reservas`, `/reservas/{id}/confirmar`, `/reservas/{id}/liberar`, `/eventos`, `/liberaciones`. | `inventario-service/src/main/java/org/example/inventarioservice/controller/InventarioController.java`, `service/InventarioService.java`, `repository/EventoAsientoRepository.java` |
| Compras | Orquesta reserva, cobro, emisión de entradas y confirmación de inventario; persiste trazabilidad de pasos y ejecuta compensaciones frente a errores. La creación exige `Idempotency-Key`. | `POST /api/compras`, `GET /api/compras/{id}`, `GET /api/compras/interno/{eventoId}/compradores`. | `compra-service/src/main/java/org/example/compraservice/controller/CompraController.java`, `service/CompraService.java`, `entity/Compra.java` |
| Pagos | Cobro idempotente, consultas por pago/cliente/compra, control de una pasarela simulada, creación y reintento de reintegros, resúmenes por compra y evento. | `/api/pagos`, `/api/pagos/cobrar`, `/reintegros`, `/reintegros/compra/{compraId}`, `/reintegros/cancelacion-evento`, `/pagos/mock`. | `pago-service/src/main/java/com/tudai/pagos/controller/PagoController.java`, `ReintegroController.java`, `service/PagoCobroTransaccionalService.java`, `pasarela/PasarelaMock.java` |
| Entradas | Emisión en lote, búsqueda por ID o cliente, cancelación individual y cancelación por evento. | `/api/entradas/emitir`, `/{id}`, `/cliente/{clienteId}`, `/{id}/cancelar`, `/eventos/{eventoId}/cancelar`. | `entrada-service/src/main/java/org/example/entradaservice/controller/EntradaController.java`, `service/EntradaService.java` |
| Fila virtual | Ingreso idempotente, consulta de posición, configuración/estado de evento, habilitación manual o automática de tandas al vencer la ventana anterior, vencimiento de autorizaciones y validación/consumo o restauración de autorización. | `/api/fila/{eventoId}/ingresos`, `/mi-posicion`; interfaces internas bajo `/api/interno/fila`. | `fila-virtual-service/src/main/java/com/tudai/filavirtualservice/controller/FilaVirtualController.java`, `FilaVirtualInternoController.java`, `service/FilaVirtualService.java`, `scheduler/TandasAutomaticasScheduler.java` |
| Notificaciones | Consume eventos, crea notificaciones pendientes, procesa y reintenta envíos, consulta detalle/listado, habilita reintento manual y avisa sobre eventos publicados a clientes con artista favorito en la misma ciudad. | `/api/notificaciones`, `/{id}`, `/{id}/reintentar`. | `notificaciones-service/src/main/java/com/tudai/notificacionesservice/messaging/EventoNotificacionListener.java`, `service/NotificacionService.java`, `client/PreferenciasClient.java`, `scheduler/NotificacionScheduler.java` |
| Recomendaciones | Persiste preferencias del cliente y recomendaciones por evento; resuelve destinatarios internos por coincidencia de artista favorito y ciudad. | `/api/preferencias`, `/api/preferencias/cliente/{clienteId}`, `/api/preferencias/interno/destinatarios`, `/api/recomendaciones/evento/{eventoId}`. | `recomendaciones-service/src/main/java/org/example/recomendacionesservice/controller/ClientePreferenciasController.java`, `service/ClientePreferenciasService.java`, `RecomendacionController.java` |
| Reportes | Configura proyección de evento/sectores, consume ventas y reintegros, y devuelve listado, resumen y detalle por sector. | `/api/reportes/eventos`, `/eventos/{id}`, `/eventos/{id}/sectores`; operaciones internas bajo `/api/reportes/interno`. | `reportes-service/src/main/java/com/tudai/reportesservice/controller/ReporteController.java`, `service/ReporteService.java`, `messaging/ReporteEventListener.java` |

No se detectaron pantallas, componentes de frontend, `package.json` ni proyectos SPA en el repositorio; las APIs constituyen la capa de interfaz implementada.

## 6. Flujo de datos e integraciones

### Compra de entradas

1. El cliente invoca `POST /api/compras` e informa la cabecera `Idempotency-Key`; el controlador recibe además identificador de cliente, email y `X-Correlation-Id` cuando está disponible. Referencia: `compra-service/src/main/java/org/example/compraservice/controller/CompraController.java`.
2. `CompraService` consulta si la clave ya fue usada; si no lo fue, crea una compra pendiente y solicita la reserva al servicio de inventario con Feign. Referencia: `compra-service/src/main/java/org/example/compraservice/service/CompraService.java` y `client/InventarioClient.java`.
3. Inventario reserva asientos o cupos, mantiene la expiración y luego puede confirmar o liberar la reserva. Referencia: `inventario-service/src/main/java/org/example/inventarioservice/service/InventarioService.java`.
4. Compra invoca al servicio de pagos. El cobro utiliza clave y hash de solicitud para resolver idempotencia; el servicio persiste el resultado y usa una pasarela en memoria simulada. Referencia: `pago-service/src/main/java/com/tudai/pagos/service/PagoCobroTransaccionalService.java` y `pasarela/PasarelaMock.java`.
5. Si el cobro se aprueba, se emiten entradas, se confirma inventario, se congelan líneas de venta y se completa la compra. Ante un error, el orquestador registra pasos y ejecuta compensaciones, incluida la liberación de la reserva. Referencia: `compra-service/src/main/java/org/example/compraservice/service/CompraService.java`.
6. Al completarse, se publican eventos RabbitMQ para notificaciones y reportes. Referencia: `compra-service/src/main/java/org/example/compraservice/messaging/CompraCompletadaPublisher.java` y `ReporteVentaPublisher.java`.

### Catálogo, inventario y fila virtual

- Publicar, suspender, reanudar o cancelar un evento ejecuta lógica en catálogo y propaga cambios hacia inventario/fila virtual mediante clientes HTTP, además de publicar eventos de notificación y reportes. Referencia: `catalogo-service/src/main/java/org/example/catalogoservice/service/EventoService.java`.
- Para eventos de alta demanda, fila virtual guarda tickets, estado y autorizaciones en Redis. `TandasAutomaticasScheduler` ejecuta cada 30 segundos por defecto y habilita una nueva tanda solamente cuando venció la ventana de compra anterior; otro scheduler vence autorizaciones pendientes. Referencia: `fila-virtual-service/src/main/java/com/tudai/filavirtualservice/redis/FilaVirtualRedisRepository.java`, `scheduler/TandasAutomaticasScheduler.java`, `scheduler/VencimientoAutorizacionesScheduler.java` y `src/main/resources/application.properties`.

### Notificación de artista favorito en ciudad

Al publicarse un evento, catálogo arma un mensaje que incluye artistas, ciudad, fecha y lugar y lo publica en RabbitMQ como `EVENTO_PUBLICADO`. Notificaciones consume el evento y consulta de forma síncrona al endpoint interno de preferencias por cada artista y ciudad. Para cada destinatario con email válido crea una notificación de tipo `ARTISTA_FAVORITO_EN_CIUDAD`, sin duplicar el mismo destinatario dentro del evento recibido. Referencias: `catalogo-service/src/main/java/org/example/catalogoservice/messaging/EventoNotificacionPublisher.java`, `notificaciones-service/src/main/java/com/tudai/notificacionesservice/service/NotificacionService.java`, `client/PreferenciasClient.java` y `recomendaciones-service/src/main/java/org/example/recomendacionesservice/controller/ClientePreferenciasController.java`.

### Eventos asíncronos

| Productor | Evento o intercambio | Consumidor | Efecto persistido |
|---|---|---|---|
| Catálogo | Eventos de catálogo y notificación, incluido `EVENTO_PUBLICADO` con artistas y ciudad | Reportes / Notificaciones | Configuración de proyección y creación de avisos a compradores o a clientes con artista favorito en esa ciudad. |
| Compras | Venta confirmada y compra completada | Reportes / Notificaciones | Métricas de venta, compradores y notificaciones. |
| Pagos | Evento de reintegro | Reportes | Devoluciones e importes de la proyección. |

La configuración de colas y exchanges se encuentra en los `RabbitMqConfig` de cada servicio. Los consumidores de notificaciones y reportes implementan persistencia de eventos recibidos/procesados para mitigar duplicados, coherente con una entrega al menos una vez. Referencias: `notificaciones-service/src/main/java/com/tudai/notificacionesservice/entity/EventoRecibido.java`, `reportes-service/src/main/java/com/tudai/reportesservice/entity/EventoProcesado.java` y `docs/servicios-fila-notificaciones-reportes.md`.

### Autenticación y autorización

`auth-service` emite JWT firmados con RS256, expone su JWK y valida solicitudes mediante filtro propio. El endpoint administrativo de usuarios exige el rol `ADMINISTRADOR`. Referencias: `auth-service/src/main/java/org/example/authservice/security/JwtKeyConfig.java`, `JwtAuthFilter.java`, `SecurityConfig.java` y `controller/UserController.java`.

`reportes-service` configura un `JwtDecoder` con el JWK de autenticación, pero su cadena de seguridad permite todas las rutas. No se identificó configuración de Spring Security equivalente en los demás servicios; por ende, la autorización no está aplicada de forma homogénea en toda la API.



