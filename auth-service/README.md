# auth-service · TicketYa

Microservicio de **Identidad y Seguridad** (arquitectura de microservicios, Spring Boot 4.1.2 / Java 21).

## Puerto y base de datos

| Recurso | Valor |
|---|---|
| Puerto HTTP | `8081` |
| BD MySQL | `ticket_ya_auth` (aislada, sin FKs hacia otros servicios) |

## Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/register` | público | Auto-registro; siempre crea rol `CLIENTE` |
| POST | `/api/auth/login` | público | Emite JWT RS256 con expiración de **1 hora** (claims `sub`, `userId` numérico, `role`) |
| GET | `/api/auth/me` | Bearer token | Datos del usuario autenticado |
| POST | `/api/auth/users` | `ADMINISTRADOR` | Alta de `ORGANIZADOR`, `ADMINISTRADOR` o `PERSONAL_ACCESO` |
| GET | `/api/auth/.well-known/jwks.json` | público | Clave pública (JWKS) para que el API Gateway valide firma y vencimiento |

Swagger UI: <http://localhost:8081/swagger-ui.html>

## Seguridad

- Contraseñas hasheadas con **BCrypt**; nunca se loguean ni se devuelven en los DTOs.
- Tokens **RS256**: la clave pública se publica vía JWKS, así el API Gateway valida firma y `exp` **sin compartir secreto** (no hace falta modificar el gateway).
- Semilla: se crea un `ADMINISTRADOR` inicial al arrancar (`auth.admin.email` / `auth.admin.password`, override con env vars `AUTH_ADMIN_EMAIL` / `AUTH_ADMIN_PASSWORD`).
- Trazabilidad: header `X-Correlation-Id` propagado a los logs (MDC) y a la respuesta.

## Integración con el API Gateway

El gateway es el **único punto expuesto** y se encarga de toda la seguridad de borde:

1. Valida firma y vencimiento del JWT contra el JWKS de este servicio. En Docker usa
   `AUTH_JWK_SET_URI=http://auth-service:8081/api/auth/.well-known/jwks.json` (en local,
   el default `http://localhost:8081/...`). No comparte secretos con los demás servicios.
2. Aplica la matriz de autorización por rol (`ROLE_<rol>` a partir del claim `role`) antes de rutear.
3. Bloquea los endpoints internos (`/api/eventos/interno/**`, `/api/compras/interno/**`,
   emisión de entradas, cobros, reintegros, etc.).
4. **Descarta** los headers `X-User-Id`, `X-User-Role` y `X-User-Email` que envíe el cliente
   y los **reinyecta desde los claims validados** (`userId`, `role`, `sub`). Así ningún
   cliente puede suplantar identidad armando headers a mano.

Este servicio solo emite y valida tokens para sus propios endpoints; los microservicios
de negocio siguen recibiendo la identidad en el formato que ya consumen (`X-User-Id` como `Long`).

## Migración de identificadores: UUID → Long

Los IDs de `Usuario` (y el claim `userId` del JWT) pasaron de `UUID` a `Long` para
coincidir con lo que esperan `compra-service` y `fila-virtual-service`
(`@RequestHeader("X-User-Id") Long clienteId`).

- **Base de datos existente:** el cambio de tipo de la columna `usuarios.id` no es
  automático. En un entorno con datos, respaldar y dropear/recrear la tabla `usuarios`
  (o migrar los IDs a enteros con una migración explícita). Con `ddl-auto=create-drop`
  en tests no hace falta ninguna acción.
- **Tokens ya emitidos:** los tokens con `userId` como UUID dejan de ser válidos para
  la identidad actual; hay que hacer `login` nuevamente para obtener un token con
  `userId` numérico. Un claim no numérico devuelve `401` (no un `500`).
- **Swagger:** los ejemplos de `id` / `userId` ahora muestran un número (p. ej. `1`).

## Ejecutar

```bash
# Local (requiere MySQL corriendo)
./mvnw spring-boot:run

# Tests (H2 en memoria)
./mvnw test

# Docker
docker build -t auth-service .
docker run -p 8081:8081 \
  -e AUTH_ADMIN_PASSWORD=admin123 \
  -e AUTH_JWT_PRIVATE_KEY="$(cat private_key.b64)" \
  -e AUTH_JWT_PUBLIC_KEY="$(cat public_key.b64)" \
  auth-service
```

### Claves RSA estables (obligatorio en Docker)

Si no se configuran claves RSA se genera un par efímero: **solo para desarrollo/tests**,
porque al reiniciar cambia el JWKS y los tokens emitidos antes dejan de validar en el
gateway y reportes (que cachean la clave pública). En `docker-compose.yml` se inyecta un
par estable y `AUTH_JWT_ALLOW_EPHEMERAL=false`, así que **sin claves el servicio no arranca**
(falla rápido, en vez de invalidar tokens silenciosamente).

Generar el par **una sola vez** y pegarlo en el `.env` (el `.env` y los `*.b64` están ignorados):

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out auth_private.pem
openssl pkcs8 -topk8 -nocrypt -in auth_private.pem -outform DER -out auth_private.der
openssl rsa -in auth_private.pem -pubout -outform DER -out auth_public.der
base64 -w0 auth_private.der > auth_private.b64
base64 -w0 auth_public.der > auth_public.b64
```

> La privada debe ser **PKCS#8** (el Base64 empieza con `MIIEv`). `openssl pkey -outform DER`
> puede entregar PKCS#1 (`MIIEp`) según la versión, y entonces `auth-service` falla al arrancar
> con `auth.jwt.private-key inválida: Unable to decode key`. Por eso se usa `pkcs8 -topk8 -nocrypt`.

Se acepta privada en Base64 **PKCS#8** y pública en Base64 **X.509**. Ver `.env.example`
en la raíz. Si se rotan las claves, publicar la nueva y la vieja en el JWKS durante más
de 60 minutos (la expiración del token) y recién retirar la vieja.

## Snippet para `docker-compose.yml` general

```yaml
  auth-service:
    build: ./auth-service
    ports:
      - "8081:8081"
    environment:
      AUTH_ADMIN_PASSWORD: ${AUTH_ADMIN_PASSWORD}
      AUTH_JWT_PRIVATE_KEY: ${AUTH_JWT_PRIVATE_KEY:?Definir en .env}
      AUTH_JWT_PUBLIC_KEY: ${AUTH_JWT_PUBLIC_KEY:?Definir en .env}
      AUTH_JWT_ALLOW_EPHEMERAL: "false"
    depends_on:
      mysql:
        condition: service_healthy
```
