package org.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.authservice.entity.Rol;

import java.time.Instant;

/**
 * Respuesta del login / alta privilegiada. Nunca incluye la contrasena.
 */
@Schema(description = "Token JWT emitido y datos del usuario autenticado.")
public record LoginResponse(
        @Schema(description = "Token JWT firmado (RS256), válido por 1 hora", example = "eyJhbGciOiJSUzI1NiJ9...")
        String token,

        @Schema(description = "Tipo de token para el header Authorization", example = "Bearer")
        String tokenType,

        @Schema(description = "Vigencia del token en segundos", example = "3600")
        long expiresIn,

        @Schema(description = "Identificador del usuario", example = "5f0c3a1e-8b7d-4c2a-9e1f-2a3b4c5d6e7f")
        java.util.UUID userId,

        @Schema(description = "Email del usuario", example = "juan.perez@mail.com")
        String email,

        @Schema(description = "Nombre del usuario", example = "Juan Pérez")
        String nombre,

        @Schema(description = "Rol asignado")
        Rol rol,

        @Schema(description = "Momento de emisión del token (ISO-8601)")
        Instant issuedAt,

        @Schema(description = "Momento de expiración del token (ISO-8601)")
        Instant expiresAt
) {
}
