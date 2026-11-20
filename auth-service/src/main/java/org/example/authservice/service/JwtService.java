package org.example.authservice.service;

import org.example.authservice.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Emisión de JWT firmados con RS256.
 *
 * <p><b>US-45</b> (criterio 2): el JWT incluye identificador, rol y vencimiento
 * de 1 hora.</p>
 *
 * <p>Claims incluidos:
 * <ul>
 *   <li>{@code sub}: email del usuario</li>
 *   <li>{@code userId}: identificador UUID</li>
 *   <li>{@code role}: rol asignado (base de los permisos de US-46 a US-49 en los demás servicios)</li>
 *   <li>{@code iat} / {@code exp}: emisión y expiración estricta (1 hora por defecto)</li>
 * </ul></p>
 */
@Service
public class JwtService {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";

    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${auth.jwt.expiration-minutes:60}") long expirationMinutes) {
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("auth.jwt.expiration-minutes debe ser mayor a 0");
        }
        this.jwtEncoder = jwtEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    /**
     * <b>US-45</b> (criterio 2): genera el token con identificador (sub/userId),
     * rol y expiración estricta de 1 hora. El claim {@code role} que emite acá
     * es el que US-46 a US-49 consumen para autorizar en cada servicio.
     */
    public String generateToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationMinutes * 60);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("auth-service")
                .subject(usuario.getEmail())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, usuario.getId().toString())
                .claim(CLAIM_ROLE, usuario.getRol().name())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long getExpiresInSeconds() {
        return expirationMinutes * 60;
    }
}
