package org.example.authservice.service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.example.authservice.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests de JwtService: claims, expiración estricta y rechazo de tokens vencidos.
 * Cobertura: US-45 (criterio 2: JWT con identificador, rol y vencimiento de 1 hora).
 */
class JwtServiceTest {

    private JwtService jwtService;
    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;
    private Usuario usuario;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID("test-rs256")
                .build();

        jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        jwtService = new JwtService(jwtEncoder, 60);
        jwtDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("juan.perez@mail.com");
        usuario.setNombre("Juan Pérez");
        usuario.setPassword("hashed");
        usuario.setRol(org.example.authservice.entity.Rol.CLIENTE);
    }

    /** US-45 (criterio 2): el token incluye identificador, rol y vencimiento de 1 hora. */
    @Test
    @DisplayName("El token firmado incluye sub, userId, role y expiración de 1 hora")
    void generaTokenConClaims() {
        String token = jwtService.generateToken(usuario);
        assertThat(token).isNotBlank();

        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("juan.perez@mail.com");
        assertThat(((Number) jwt.getClaim(JwtService.CLAIM_USER_ID)).longValue()).isEqualTo(usuario.getId());
        assertThat(jwt.getClaimAsString(JwtService.CLAIM_ROLE)).isEqualTo("CLIENTE");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
        assertThat(jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond()).isEqualTo(3600L);
    }

    /** US-45 (criterio 2): vencimiento estricto de 1 hora — un token vencido no decodifica. */
    @Test
    @DisplayName("Un token con expiración vencida no decodifica")
    void rechazaTokenExpirado() {
        // Token emitido 2 horas atrás con vigencia de 1 hora => ya venció
        java.time.Instant emitido = java.time.Instant.now().minusSeconds(7200);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getEmail())
                .claim(JwtService.CLAIM_USER_ID, usuario.getId())
                .claim(JwtService.CLAIM_ROLE, usuario.getRol().name())
                .issuedAt(emitido)
                .expiresAt(emitido.plusSeconds(3600))
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        assertThatThrownBy(() -> jwtDecoder.decode(token))
                .isInstanceOf(JwtValidationException.class);
    }
}
