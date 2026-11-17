package org.example.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.authservice.dto.CreateUserRequest;
import org.example.authservice.dto.UsuarioResponse;
import org.example.authservice.service.AuthService;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alta privilegiada de cuentas (US-45): solo ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Administración de usuarios", description = "Alta de cuentas privilegiadas (solo ADMINISTRADOR)")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Alta privilegiada de usuarios",
            description = "Un ADMINISTRADOR puede dar de alta cuentas con roles ORGANIZADOR, ADMINISTRADOR o PERSONAL_ACCESO. "
                    + "No permite crear CLIENTE (para eso existe el auto-registro público).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada con el rol indicado",
                    headers = @Header(name = "X-Correlation-Id", description = "Identificador de correlación para trazas"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UsuarioResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": "8a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
                                      "email": "maria.gomez@mail.com",
                                      "nombre": "María Gómez",
                                      "rol": "ORGANIZADOR",
                                      "activo": true,
                                      "fechaCreacion": "2026-11-16T12:00:00Z"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o rol CLIENTE solicitado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Validación fallida",
                                      "status": 400,
                                      "detail": "La solicitud contiene datos inválidos",
                                      "errores": { "rol": "el rol es obligatorio" }
                                    }"""))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El token no corresponde a un ADMINISTRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Prohibido",
                                      "status": 403,
                                      "detail": "No tiene permisos para realizar esta operación"
                                    }"""))),
            @ApiResponse(responseCode = "409", description = "Email ya registrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UsuarioResponse created = authService.createPrivileged(request);
        return ResponseEntity.status(201).body(created);
    }
}
