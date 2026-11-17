package org.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales de inicio de sesión.")
public record LoginRequest(
        @Schema(description = "Email del usuario", example = "juan.perez@mail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "el email es obligatorio")
        @Email(message = "el email no tiene un formato válido")
        String email,

        @Schema(description = "Contraseña en texto plano", example = "Secreta1!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "la contraseña es obligatoria")
        String password
) {
}
