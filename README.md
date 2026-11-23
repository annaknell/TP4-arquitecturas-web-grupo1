## Guía de Ejecución, descripción del diseño, configuración de Entorno y Especificaciones

La arquitectura y criterios de Fila Virtual, Notificaciones y Reportes de la entrega 2 están documentados en `docs/servicios-fila-notificaciones-reportes.md`.

### Requisitos Previos
- **Java SDK:** 17 o superior.
- **Maven:** Para compilación y resolución de dependencias (`pom.xml`).
- **Docker / Docker Compose:** Para levantar la plataforma completa (infraestructura y microservicios).
- **Postman / curl:** Para ejecutar las pruebas de la API.

### Paso 1: Configuración de Entorno (`.env` y JWT)
1. Copiá `.env.example` a `.env` y completá las variables.
2. Configurá las claves RSA estables `AUTH_JWT_PRIVATE_KEY` y `AUTH_JWT_PUBLIC_KEY` exigidas por `auth-service` (el archivo `.env` y los `*.b64` están ignorados por Git).
   - *Importante:* Sin esas claves el compose falla con un mensaje claro. Nunca se genera un par efímero en Docker, porque eso cambiaría el JWKS e invalidaría los tokens vigentes al reiniciar.

### Paso 2: Levantar la Plataforma con Docker Compose
Desde la raíz del proyecto, ejecutá:

bash
docker-compose up -d'

### Paso 3: Ejecución Manual / Depuración en IntelliJ IDEA (Opcional)
Si necesitás ejecutar o depurar algún microservicio puntual fuera de Docker (deteniendo primero su contenedor y ejecutando su clase \*Application desde el botón verde de IntelliJ IDEA):
- Levantar las dependencias de infraestructura (bases de datos, Redis, MongoDB/RabbitMQ según el servicio) y el resto de los servicios necesarios.
- Si levantás o reiniciás el API Gateway (habitualmente en el puerto 8080), hacelo al final, una vez que los servicios de negocio estén arriba.

### Paso 4: Pruebas y Consultas
- Una vez que el API Gateway esté corriendo (generalmente en el puerto 8080), abrí Postman (o usá curl), importá la colección de requests y consultá el sistema apuntando siempre al Gateway (salvo endpoints internos explícitos en pruebas locales).
- Revisar los criterios y flujos en docs/servicios-fila-notificaciones-reportes.md y docs/features-us.md.

### Descripción del Diseño
El sistema está organizado como una arquitectura de microservicios detrás de un API Gateway, con autenticación y autorización centralizada (auth-service con claves RSA/JWKS) y servicios de dominio desacoplados: catálogo, inventario, entradas, compra/reserva, pago, fila virtual, notificaciones y reportes. Combina persistencia relacional, almacenamiento NoSQL y Redis según el caso, junto con mensajería y eventos para desacoplar procesos y notificaciones. Los detalles de arquitectura, flujos y contratos están documentados en docs/.

### Configuración de Entorno, Resiliencia y Reintentos
Timeouts HTTP: feign.client.config.default.connectTimeout/readTimeout en compra-service y spring.http.clients.connect-timeout/read-timeout (2s/3s) en catálogo, inventario, notificaciones y entradas. Estos últimos se aplican porque los clientes usan el RestClient.Builder inyectado por Spring.
Circuit Breaker / Reintentos: compra-service reintenta el cobro (3 intentos con backoff exponencial, seguro por Idempotency-Key) y con el circuito abierto deja la compra en PAGO_INCIERTO para reconciliación; nunca aprueba un pago por fallback. Catálogo y notificaciones usan circuit breaker con fallback para no bloquear ni perder avisos. Los reintentos de emails de US-38 siguen en notificaciones-service (no se duplican).

### Especificaciones y Desarrollo Guiado por Especificaciones
En algunas partes del código se adoptó desarrollo guiado por especificaciones: docs/features-us.md y los contratos de docs/ fueron el insumo para los agentes de IA que asistieron en la implementación. No se utilizó el CLI de OpenSpec, sino la práctica equivalente con especificaciones versionadas.
