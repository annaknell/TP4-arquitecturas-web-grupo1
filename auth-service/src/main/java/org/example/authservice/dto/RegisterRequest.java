package org.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request de auto-registro publico (US-45). El rol asignado siempre es CLIENTE
 * y por eso NO forma parte de este request.
 */
@Schema(description = "Datos de auto-registro público. El rol asignado es siempre CLIENTE.")
public record RegisterRequest(
        @Schema(description = "Nombre del usuario", example = "Juan Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "el nombre no puede superar los 120 caracteres")
        String nombre,

        @Schema(description = "Email del usuario", example = "juan.perez@mail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "el email es obligatorio")
        @Email(message = "el email no tiene un formato válido")
        @Size(max = 180, message = "el email no puede superar los 180 caracteres")
        String email,

        @Schema(description = "Contraseña en texto plano (se almacena hasheada con BCrypt)", example = "Secreta1!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "la contraseña debe tener entre 8 y 72 caracteres")
        String password
) {
}
