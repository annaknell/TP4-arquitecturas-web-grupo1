package org.example.authservice.service;

import org.example.authservice.dto.CreateUserRequest;
import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LoginResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.dto.UsuarioResponse;
import org.example.authservice.entity.Rol;
import org.example.authservice.entity.Usuario;
import org.example.authservice.exception.DuplicateEmailException;
import org.example.authservice.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Lógica de identidad: auto-registro, login y alta privilegiada.
 *
 * <p>Cobertura de User Stories (F12 · Identidad y permisos):</p>
 * <ul>
 *   <li><b>US-45</b> (E2 · Seguridad): registro de clientes, login con JWT,
 *       alta de roles privilegiados por un administrador, hash de contraseñas.</li>
 *   <li><b>US-48</b> (E2 · Seguridad): el ADMINISTRADOR es quien gestiona usuarios
 *       (criterio 1: "gestionar usuarios"); aquí se concreta vía {@link #createPrivileged}.</li>
 * </ul>
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * <b>US-45</b> (criterio 1): "Los clientes se registran por sí mismos".
     * Auto-registro público: siempre crea cuentas CLIENTE.
     */
    @Transactional
    public UsuarioResponse register(RegisterRequest request) {
        return saveUser(request.nombre(), request.email(), request.password(), Rol.CLIENTE);
    }

    /**
     * <b>US-45</b> (criterio 1): "Organizadores, administradores y personal de acceso son
     * dados de alta por un administrador".
     * <b>US-48</b> (criterio 1): "gestionar usuarios" es función del ADMINISTRADOR
     * (el acceso al endpoint se restringe con @PreAuthorize en UserController).
     * Rechaza el rol CLIENTE: para ese rol existe el auto-registro público.
     */
    @Transactional
    public UsuarioResponse createPrivileged(CreateUserRequest request) {
        if (request.rol() == Rol.CLIENTE) {
            throw new IllegalArgumentException("CLIENTE no puede asignarse en el alta privilegiada: use el registro público /api/auth/register");
        }
        return saveUser(request.nombre(), request.email(), request.password(), request.rol());
    }

    /**
     * <b>US-45</b> (criterio 2): "Dado credenciales válidas, cuando inicio sesión, entonces
     * recibo un JWT con identificador, rol y vencimiento (1 hora). Con credenciales
     * inválidas recibo 401".
     * <b>US-45</b> (criterio 4): "Las contraseñas se guardan con hash y nunca aparecen en
     * logs" (la comparación se hace contra el hash BCrypt; los mensajes de error no
     * distinguen email inexistente de contraseña incorrecta).
     * Valida credenciales y emite un JWT firmado con vigencia estricta de 1 hora.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        if (!usuario.isActivo()) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        Instant issuedAt = Instant.now();
        String token = jwtService.generateToken(usuario);
        Instant expiresAt = issuedAt.plusSeconds(jwtService.getExpiresInSeconds());

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpiresInSeconds(),
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol(),
                issuedAt,
                expiresAt
        );
    }

    private UsuarioResponse saveUser(String nombre, String email, String password, Rol rol) {
        String normalizedEmail = email.trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setEmail(normalizedEmail);
        // BCrypt: la contraseña nunca se persiste ni se loguea en claro.
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(rol);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }
}
