package org.example.authservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.example.authservice.repository.UsuarioRepository;
import org.example.authservice.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración (MockMvc + Spring Security Test).
 *
 * <p>Cobertura: <b>US-45</b> (registro, login, JWT de 1 hora, hash de
 * contraseñas, JWKS), <b>criterio transversal US-46 a US-49</b> (401 con token
 * ausente/inválido/vencido; 403 con rol sin permiso) y <b>EN-13</b> (criterio 2:
 * pruebas de seguridad que verifican que cada endpoint solo es accesible por
 * los roles correspondientes).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@ticketya.com";
    private static final String ADMIN_PASSWORD = "Admin123!";

    @Autowired
    private MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.nimbusds.jose.jwk.RSAKey jwtRsaKey;

    private String adminToken;
    private String clienteToken;

    @AfterEach
    void cleanup() {
        // El admin semilla se recrea si corresponde; se limpian los usuarios de prueba
        usuarioRepository.findAll()
                .forEach(u -> {
                    if (!ADMIN_EMAIL.equals(u.getEmail())) {
                        usuarioRepository.delete(u);
                    }
                });
    }

    @BeforeEach
    void loginFixtures() throws Exception {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        registerClient();
        clienteToken = login("juan.perez@mail.com", "Secreta1!");
    }

    // ------------------------------------------------------------------
    // Registro público
    // ------------------------------------------------------------------

    /** US-45 (criterios 1 y 4): registro público, rol CLIENTE, password hasheada en BD. */
    @Test
    @DisplayName("register: 201 crea cuenta CLIENTE y la contraseña queda hasheada en BD")
    void registerCreaCliente() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "test-corr-001")
                        .content("""
                                {"nombre":"Ana López","email":"ana.lopez@mail.com","password":"Secreta1!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", "test-corr-001"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.email").value("ana.lopez@mail.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        var guardado = usuarioRepository.findByEmailIgnoreCase("ana.lopez@mail.com").orElseThrow();
        assertThat(guardado.getPassword()).startsWith("$2"); // hash BCrypt
        assertThat(guardado.getPassword()).isNotEqualTo("Secreta1!");
    }

    /** US-45 (criterio 1): unicidad de cuenta por email. */
    @Test
    @DisplayName("register: 409 si el email ya está registrado")
    void registerEmailDuplicado() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Juan Otra Vez","email":"juan.perez@mail.com","password":"Secreta1!"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email duplicado"));
    }

    /** US-45 (criterio 2): datos de registro validados → 400. */
    @Test
    @DisplayName("register: 400 con datos inválidos")
    void registerDatosInvalidos() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","email":"no-es-email","password":"corta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").exists());
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    /** US-45 (criterio 2): login emite JWT con identificador, rol y vencimiento de 1 hora. */
    @Test
    @DisplayName("login: 200 emite JWT con vigencia de 1 hora")
    void loginEmitToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"juan.perez@mail.com","password":"Secreta1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.get("token").asText();
        assertThat(token.split("\\.")).hasSize(3); // formato JWT
        Instant issuedAt = Instant.parse(body.get("issuedAt").asText());
        Instant expiresAt = Instant.parse(body.get("expiresAt").asText());
        assertThat(expiresAt.getEpochSecond() - issuedAt.getEpochSecond()).isEqualTo(3600L);
    }

    /** US-45 (criterio 2): credenciales inválidas → 401. */
    @Test
    @DisplayName("login: 401 con contraseña incorrecta")
    void loginPasswordIncorrecta() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"juan.perez@mail.com","password":"Incorrecta1!"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    /** US-45 (criterio 2): credenciales inválidas → 401 (email inexistente). */
    @Test
    @DisplayName("login: 401 con email inexistente")
    void loginEmailInexistente() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nadie@mail.com","password":"Secreta1!"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Endpoint protegido /api/auth/me
    // ------------------------------------------------------------------

    /** US-45 + criterio transversal US-46 a US-49: token válido accede; sin token → 401. */
    @Test
    @DisplayName("me: 200 con token válido y 401 sin token")
    void meConYSinToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan.perez@mail.com"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    /** Criterio transversal US-46 a US-49: token vencido → 401. */
    @Test
    @DisplayName("me: 401 con token expirado")
    void meTokenExpirado() throws Exception {
        String vencido = tokenConVigencia(-2);
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized());
    }

    /** Criterio transversal US-46 a US-49: token inválido (firma) → 401. */
    @Test
    @DisplayName("me: 401 con token firmado con otra clave (tampering)")
    void meTokenFirmaInvalida() throws Exception {
        String otro = tokenConOtraClave();
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + otro))
                .andExpect(status().isUnauthorized());
    }

    /** Migración UUID → Long: un claim userId legacy debe dar 401, no un 500. */
    @Test
    @DisplayName("me: 401 con token legacy de userId UUID")
    void meTokenUserIdLegacy() throws Exception {
        String legacy = tokenConUserId(java.util.UUID.randomUUID().toString());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + legacy))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Alta privilegiada POST /api/auth/users (solo ADMINISTRADOR)
    // ------------------------------------------------------------------

    /**
     * US-48 (criterio 1) + US-45 (criterio 1) + EN-13 (criterio 2):
     * el ADMINISTRADOR gestiona usuarios (alta de ORGANIZADOR/Admin/Personal).
     */
    @Test
    @DisplayName("users: 201 con token de ADMINISTRADOR y rol ORGANIZADOR")
    void usersConAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"María Gómez","email":"maria.gomez@mail.com","password":"Secreta1!","rol":"ORGANIZADOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ORGANIZADOR"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /** US-46 (criterio 2) + transversal US-46 a US-49: CLIENTE sin permiso → 403. */
    @Test
    @DisplayName("users: 403 con token de CLIENTE")
    void usersConCliente() throws Exception {
        mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Intruso","email":"intruso@mail.com","password":"Secreta1!","rol":"ADMINISTRADOR"}
                                """))
                .andExpect(status().isForbidden());
    }

    /** Criterio transversal US-46 a US-49: sin token → 401. */
    @Test
    @DisplayName("users: 401 sin token")
    void usersSinToken() throws Exception {
        mockMvc.perform(post("/api/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"X","email":"x@mail.com","password":"Secreta1!","rol":"PERSONAL_ACCESO"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    /** US-45 (criterio 1): el alta privilegiada no crea CLIENTE (para eso existe /register). */
    @Test
    @DisplayName("users: 400 si se intenta crear rol CLIENTE")
    void usersRolClienteRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cliente Vía Admin","email":"cliente.admin@mail.com","password":"Secreta1!","rol":"CLIENTE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // JWKS público
    // ------------------------------------------------------------------

    /** US-45 (criterio 3): JWKS permite al gateway validar firma y vencimiento sin secretos compartidos. */
    @Test
    @DisplayName("jwks: expone la clave pública sin material privado")
    void jwksPublico() throws Exception {
        mockMvc.perform(get("/api/auth/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist()); // la clave privada NO se expone
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void registerClient() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Juan Pérez","email":"juan.perez@mail.com","password":"Secreta1!"}
                                """))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    /** Token firmado con la clave del servicio pero emitido hace {@code offsetHoras} con vigencia de 1 h. */
    private String tokenConVigencia(int offsetHoras) {
        Instant emitido = Instant.now().plusSeconds(offsetHoras * 3600L);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("juan.perez@mail.com")
                .claim(JwtService.CLAIM_USER_ID, 1L)
                .claim(JwtService.CLAIM_ROLE, "CLIENTE")
                .issuedAt(emitido)
                .expiresAt(emitido.plusSeconds(3600))
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwtRsaKey)));
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /** Token válido con un claim userId arbitrario (para simular formatos legacy). */
    private String tokenConUserId(Object userId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("juan.perez@mail.com")
                .claim(JwtService.CLAIM_USER_ID, userId)
                .claim(JwtService.CLAIM_ROLE, "CLIENTE")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwtRsaKey)));
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /** Token firmado con un par de claves distinto al del servicio. */
    private String tokenConOtraClave() throws Exception {
        java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keyPair = generator.generateKeyPair();
        RSAKey otra = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID("otra-clave")
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(otra)));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("atacante@mail.com")
                .claim(JwtService.CLAIM_USER_ID, 1L)
                .claim(JwtService.CLAIM_ROLE, "ADMINISTRADOR")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
