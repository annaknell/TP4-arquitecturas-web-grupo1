package org.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.authservice.entity.Rol;

/**
 * Alta privilegiada de usuarios (US-45). Solo ADMINISTRADOR.
 * No permite crear CLIENTE: para eso existe el auto-registro público.
 */
@Schema(description = "Alta privilegiada de cuentas con roles ORGANIZADOR, ADMINISTRADOR o PERSONAL_ACCESO. Requiere rol ADMINISTRADOR.")
public record CreateUserRequest(
        @Schema(description = "Nombre del usuario", example = "María Gómez", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "el nombre no puede superar los 120 caracteres")
        String nombre,

        @Schema(description = "Email del usuario", example = "maria.gomez@mail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "el email es obligatorio")
        @Email(message = "el email no tiene un formato válido")
        @Size(max = 180, message = "el email no puede superar los 180 caracteres")
        String email,

        @Schema(description = "Contraseña en texto plano (se almacena hasheada con BCrypt)", example = "Secreta1!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "la contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @Schema(description = "Rol a asignar. CLIENTE no está permitido (usar el registro público).", allowableValues = {"ORGANIZADOR", "ADMINISTRADOR", "PERSONAL_ACCESO"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "el rol es obligatorio")
        Rol rol
) {
}
