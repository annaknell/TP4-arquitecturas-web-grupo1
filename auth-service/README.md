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
| POST | `/api/auth/login` | público | Emite JWT RS256 con expiración de **1 hora** (claims `sub`, `userId`, `role`) |
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

1. Agregar ruta al `docker-compose` general / `application.yml` del gateway apuntando a `http://localhost:8081` con `Path=/api/auth/**`.
2. El gateway valida el JWT decodificando la clave desde el JWKS (`http://localhost:8081/api/auth/.well-known/jwks.json`) y luego propaga `X-User-Id` y `X-User-Role` a los demás microservicios (claims `userId` y `role`).

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

> Si no se configuran claves RSA, se genera un par efímero (solo para desarrollo: al reiniciar, los tokens previos dejan de ser válidos).

## Snippet para `docker-compose.yml` general

```yaml
  auth-service:
    build: ./auth-service
    ports:
      - "8081:8081"
    environment:
      AUTH_ADMIN_PASSWORD: ${AUTH_ADMIN_PASSWORD}
      AUTH_JWT_PRIVATE_KEY: ${AUTH_JWT_PRIVATE_KEY}
      AUTH_JWT_PUBLIC_KEY: ${AUTH_JWT_PUBLIC_KEY}
    depends_on:
      mysql:
        condition: service_healthy
```
