package org.example.authservice.controller;

import com.nimbusds.jose.jwk.JWKSet;
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
import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LoginResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.dto.UsuarioResponse;
import org.example.authservice.entity.Usuario;
import org.example.authservice.repository.UsuarioRepository;
import org.example.authservice.service.AuthService;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * Endpoints públicos de identidad (US-45): auto-registro, login y JWKS.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Auto-registro público, inicio de sesión con JWT y clave pública para el API Gateway")
public class AuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;
    private final JWKSet publicJwkSet;

    public AuthController(AuthService authService, UsuarioRepository usuarioRepository, JWKSet publicJwkSet) {
        this.authService = authService;
        this.usuarioRepository = usuarioRepository;
        this.publicJwkSet = publicJwkSet;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Auto-registro público",
            description = "Cualquier usuario puede registrarse por sí mismo. La cuenta se crea siempre con rol CLIENTE. "
                    + "Roles privilegiados (ORGANIZADOR, ADMINISTRADOR, PERSONAL_ACCESO) solo pueden darse de alta mediante POST /api/auth/users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada con rol CLIENTE",
                    headers = @Header(name = "X-Correlation-Id", description = "Identificador de correlación para trazas"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UsuarioResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": "5f0c3a1e-8b7d-4c2a-9e1f-2a3b4c5d6e7f",
                                      "email": "juan.perez@mail.com",
                                      "nombre": "Juan Pérez",
                                      "rol": "CLIENTE",
                                      "activo": true,
                                      "fechaCreacion": "2026-11-16T12:00:00Z"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (email mal formado, contraseña corta, etc.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Validación fallida",
                                      "status": 400,
                                      "detail": "La solicitud contiene datos inválidos",
                                      "errores": { "email": "el email no tiene un formato válido" }
                                    }"""))),
            @ApiResponse(responseCode = "409", description = "Ya existe una cuenta con ese email",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Email duplicado",
                                      "status": 409,
                                      "detail": "Ya existe una cuenta registrada con el email: juan.perez@mail.com"
                                    }""")))
    })
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegisterRequest request) {
        UsuarioResponse created = authService.register(request);
        return ResponseEntity.created(URI.create("/api/auth/me")).body(created);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Inicio de sesión",
            description = "Valida credenciales y emite un JWT firmado (RS256) con expiración estricta de 1 hora. "
                    + "Incluye los claims sub (email), userId, role, iat y exp."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso, devuelve el token JWT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "token": "eyJhbGciOiJSUzI1NiJ9...",
                                      "tokenType": "Bearer",
                                      "expiresIn": 3600,
                                      "userId": "5f0c3a1e-8b7d-4c2a-9e1f-2a3b4c5d6e7f",
                                      "email": "juan.perez@mail.com",
                                      "nombre": "Juan Pérez",
                                      "rol": "CLIENTE",
                                      "issuedAt": "2026-11-16T12:00:00Z",
                                      "expiresAt": "2026-11-16T13:00:00Z"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Campos vacíos o email mal formado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "type": "about:blank",
                                      "title": "No autorizado",
                                      "status": 401,
                                      "detail": "Credenciales inválidas"
                                    }""")))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Usuario autenticado actual",
            description = "Endpoint protegido: valida el token Bearer (firma y expiración) y devuelve los datos del usuario.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UsuarioResponse> me(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Usuario inexistente"));
        return ResponseEntity.ok(UsuarioResponse.from(usuario));
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(
            summary = "Clave pública (JWKS)",
            description = "Expone la clave pública RSA en formato JWKS para que el API Gateway valide la firma y el "
                    + "vencimiento de los tokens sin necesidad de compartir un secreto. La clave privada nunca se expone."
    )
    @ApiResponse(responseCode = "200", description = "JWKSet público",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = """
                            {
                              "keys": [
                                {
                                  "kty": "RSA",
                                  "kid": "auth-service-rs256",
                                  "alg": "RS256",
                                  "use": "sig",
                                  "n": "...",
                                  "e": "AQAB"
                                }
                              ]
                            }""")))
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok(publicJwkSet.toJSONObject(true));
    }
}
