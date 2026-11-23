**Guía de Ejecución**

La arquitectura y criterios de Fila Virtual, Notificaciones y Reportes de la entrega 2 están documentados en `docs/servicios-fila-notificaciones-reportes.md`.

Requisitos Previos
- Java SDK: 17 o superior.
- Maven: Para la resolución de dependencias (pom.xml).
- Docker / Docker Compose: Para levantar los contenedores de las bases de datos relacionales y Redis.
- Postman: Para importar la colección de pruebas de la API.

- Paso 1: Levantar las Bases de Datos y Redis
  Desde la terminal en la raíz del proyecto asegurate de levantar los servicios de persistencia: **docker-compose up -d**

- Paso 2: Ejecución de los Microservicios
  En IntelliJ IDEA, abrís cada módulo y ejecutás el método main de su clase de configuración principal (haciendo clic en el botón de Play verde al lado de la clase):
  Catálogo: Ejecutar CatalogoServiceApplication
  Entradas: Ejecutar EntradaServiceApplication
  Compra / Reserva: Ejecutar CompraServiceApplication (o ReservaServiceApplication)
  Pago (Mock): Ejecutar PagoServiceApplication
  API Gateway: (Debe ser el último en levantarse) Ejecutar ApiGatewayApplication
  Importante: Levantar todos los microservicios de negocio primero y dejar el API Gateway para el final.
  
- Paso 3: Realizar Consultas y Pruebas
  Una vez que el API Gateway esté corriendo (generalmente en el puerto 8080), abrir Postman, importar la colección de requests y empezar a interactuar con el sistema apuntando siempre al Gateway.

Configuración de entorno
- Copiar `.env.example` a `.env` y completar las variables. El `auth-service` exige las claves RSA estables `AUTH_JWT_PRIVATE_KEY` / `AUTH_JWT_PUBLIC_KEY` (el `.env` y los `*.b64` están ignorados por Git). Sin esas claves el compose falla con un mensaje claro: nunca se genera un par efímero en Docker, porque eso cambiaría el JWKS e invalidaría los tokens vigentes al reiniciar.
- Timeouts HTTP: `feign.client.config.default.connectTimeout/readTimeout` en `compra-service` y `spring.http.clients.connect-timeout/read-timeout` (2s/3s) en catálogo, inventario, notificaciones y entradas. Estos últimos se aplican porque los clientes usan el `RestClient.Builder` inyectado por Spring.
- Circuit breaker / reintentos: `compra-service` reintenta el cobro (3 intentos con backoff exponencial, seguro por `Idempotency-Key`) y con el circuito abierto deja la compra en `PAGO_INCIERTO` para reconciliación; nunca aprueba un pago por fallback. Catálogo y notificaciones usan circuit breaker con fallback para no bloquear ni perder avisos. Los reintentos de emails de US-38 siguen en `notificaciones-service` (no se duplican).


