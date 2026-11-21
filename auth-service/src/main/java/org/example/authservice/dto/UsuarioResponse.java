package org.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.authservice.entity.Rol;
import org.example.authservice.entity.Usuario;

import java.time.Instant;

/**
 * Datos públicos de un usuario. Nunca incluye la contrasena.
 */
@Schema(description = "Datos públicos de un usuario (sin información sensible).")
public record UsuarioResponse(
        @Schema(description = "Identificador del usuario", example = "1")
        Long id,

        @Schema(description = "Email del usuario", example = "juan.perez@mail.com")
        String email,

        @Schema(description = "Nombre del usuario", example = "Juan Pérez")
        String nombre,

        @Schema(description = "Rol asignado")
        Rol rol,

        @Schema(description = "Indica si la cuenta está activa")
        boolean activo,

        @Schema(description = "Fecha de creación de la cuenta (ISO-8601)")
        Instant fechaCreacion
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol(),
                usuario.isActivo(),
                usuario.getFechaCreacion()
        );
    }
}
