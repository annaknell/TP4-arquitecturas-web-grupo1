Informe Técnico — Entrega 1 grupo 1

**1. Introducción y Alcance**
TicketYa es una plataforma orientada a la comercialización masiva de entradas para eventos (recitales, festivales, obras de teatro, deportes y conferencias). Su principal desafío arquitectónico radica en soportar picos altos de concurrencia al abrirse la venta de eventos convocantes, garantizando consistencia transaccional, equidad y la prevención estricta de la doble venta de localidades.
Esta Entrega 1 (Core) se enfoca en la implementación de los cimientos del sistema:
Gestión y publicación de eventos y lugares con sectores numerados y no numerados.
El circuito transaccional de compra: reserva temporal, procesamiento con una pasarela de pagos simulada (mock) y emisión de entradas con códigos QR únicos.
Mecanismos de concurrencia para evitar la doble venta bajo alta demanda.
Búsqueda multicriterio de eventos.

**2. Arquitectura de Solución**
El sistema se diseñó bajo una arquitectura de microservicios desacoplada, implementada con Spring Boot, donde cada microservicio encapsula su propia lógica de negocio y gestiona un esquema de persistencia independiente. El acceso externo está centralizado a través de un API Gateway.

api-gateway: 
- Punto de entrada único, enrutamiento dinámico y balanceo de carga.
- Sin persistencia (Stateless)

catalogo-service:
- Administración de eventos, lugares, sectores, precios y motor de búsquedas multicriterio (Req. 1, 2, 3, 9).
- Base de datos relacional dedicada (db_catalogo)

reserva-service:
- Orquestación del flujo de compra, control de concurrencia y gestión de reservas temporales (Req. 4, 5).
- Base de datos relacional (db_reserva) + Redis (para TTL de reservas)

entrada-service:
- Generación de entradas con código QR único, registro de compras y gestión de devoluciones (Req. 4, 7).
- Base de datos relacional (db_entrada)

pago-mock-service:
- Simulación síncrona de la pasarela de pagos externa con respuestas configurables de éxito/fallo.
- Sin persistencia (Stateless)

**3. Estrategia contra la Doble Venta (Requerimiento Crítico)**
Para dar cumplimiento estricto a la regla de negocio que impide vender una misma entrada dos veces bajo alta concurrencia simultánea, se implementó una estrategia híbrida:
Para Sectores Numerados (Asientos específicos):
Se utiliza Bloqueo Pesimista (Pessimistic Locking mediante SELECT ... FOR UPDATE a nivel de base de datos)sobre la entidad que representa el asiento o localidad, asegurando exclusividad transaccional mientras se procesa la validación y el pago.
Complementariamente, se utiliza Redis para registrar el estado temporal del asiento (RESERVADO) con un tiempo de vida (TTL de 10 minutos). Si el pago no se completa en ese lapso o es rechazado, la reserva expira automáticamente, liberando el recurso.
Para Sectores No Numerados (Capacidad general / Campo):
Se implementa control atómico de decremento de stock mediante actualizaciones optimizadas en la base de datos o control de versiones (@Version de JPA), evitando condiciones de carrera (race conditions) cuando múltiples clientes adquieren los últimos lugares disponibles de una zona simultáneamente.

**4. Flujo de Transacción de Compra (Diagrama de Secuencia Conceptual)**
El cliente envía la solicitud de compra a través del API Gateway hacia el reserva-service.
El reserva-service bloquea el asiento/capacidad y genera una reserva temporal en Redis con TTL.
Se invoca de forma síncrona al pago-mock-service para procesar el cobro.
Si el pago es aprobado: Se comunica con el entrada-service para emitir formalmente la entrada con su código QR único y se persiste la compra exitosa.
Si el pago es rechazado o expira el tiempo: Se ejecuta una compensación liberando inmediatamente el bloqueo del asiento y el stock en el sistema.

**5. Diseño de API REST y Contratos**
Las interfaces de comunicación entre microservicios y clientes externos se basan en APIs REST síncronas.
Las especificaciones de los contratos se documentaron formalmente mediante OpenAPI (Swagger), detallando endpoints, verbos HTTP (GET, POST), esquemas de objetos JSON para payloads y códigos de estado HTTP estándar (201 Created, 400 Bad Request, 409 Conflict).

**6. Conclusiones y Próximos Pasos (Entrega 2)**
La arquitectura modular implementada en esta primera etapa garantiza la independencia de despliegue de los servicios, la escalabilidad del núcleo de ventas y la robustez ante escenarios de alta concurrencia. De cara a la Entrega 2, el sistema se completará incorporando la cola virtual para eventos masivos, seguridad basada en JWT con roles, persistencia NoSQL para analítica/preferencias, trazabilidad centralizada de logs y contenedores Docker orquestados mediante docker-compose.

