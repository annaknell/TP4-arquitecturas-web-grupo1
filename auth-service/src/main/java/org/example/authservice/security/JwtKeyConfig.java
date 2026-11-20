package org.example.authservice.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

/**
 * Configuración del par de claves RSA (RS256) para firmar y validar JWT.
 *
 * <p>Las claves se leen de la configuración (Base64). Si no están definidas,
 * se genera un par efímero para desarrollo. La clave PÚBLICA se expone vía
 * el endpoint JWKS para que el API Gateway valide firma y vencimiento.</p>
 */
@Configuration
public class JwtKeyConfig {

    @Value("${auth.jwt.key-id:auth-service-rs256}")
    private String keyId;

    /** Clave privada en Base64 (formato PKCS#8). Opcional en desarrollo. */
    @Value("${auth.jwt.private-key:}")
    private String privateKeyBase64;

    /** Clave pública en Base64 (formato X.509). Opcional en desarrollo. */
    @Value("${auth.jwt.public-key:}")
    private String publicKeyBase64;

    @Bean
    public KeyPair jwtKeyPair() {
        if (!privateKeyBase64.isBlank() && !publicKeyBase64.isBlank()) {
            return new KeyPair(parsePublicKey(publicKeyBase64), parsePrivateKey(privateKeyBase64));
        }
        // Par efímero: solo para desarrollo/tests. Los tokens emitidos con
        // una instancia anterior dejan de ser válidos al reiniciar.
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA no está disponible", e);
        }
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    /** JWK con clave privada y pública: usado por el JwtEncoder para firmar. */
    @Bean
    public RSAKey jwtRsaKey(KeyPair jwtKeyPair) {
        return new RSAKey.Builder((RSAPublicKey) jwtKeyPair.getPublic())
                .privateKey(jwtKeyPair.getPrivate())
                .keyID(keyId)
                .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                .build();
    }

    /** JWKSet pública (sin material privado) expuesta en el endpoint JWKS. */
    @Bean
    public JWKSet publicJwkSet(RSAKey jwtRsaKey) {
        return new JWKSet(jwtRsaKey.toPublicJWK());
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey jwtRsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwtRsaKey)));
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyPair jwtKeyPair) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) jwtKeyPair.getPublic()).build();
    }

    private static java.security.PrivateKey parsePrivateKey(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64.trim());
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("auth.jwt.private-key inválida: " + e.getMessage(), e);
        }
    }

    private static java.security.PublicKey parsePublicKey(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64.trim());
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("auth.jwt.public-key inválida: " + e.getMessage(), e);
        }
    }
}
