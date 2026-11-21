package org.example.apigatewayservice.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifica que la identidad que llega a los microservicios se construye desde
 * el JWT y nunca desde los headers que envía el cliente.
 */
class IdentityHeadersFilterTest {

    private final IdentityHeadersFilter filter = new IdentityHeadersFilter();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Descarta los headers del cliente y los reinyecta desde los claims")
    void reconstruyeIdentidadDesdeElJwt() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("juan.perez@mail.com")
                .claim("userId", 42L)
                .claim("role", "CLIENTE")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")), jwt.getSubject()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(IdentityHeadersFilter.HEADER_USER_ID, "999");
        request.addHeader(IdentityHeadersFilter.HEADER_USER_ROLE, "ADMINISTRADOR");
        request.addHeader(IdentityHeadersFilter.HEADER_USER_EMAIL, "atacante@mail.com");

        HttpServletRequest reenviada = reenviar(request);

        assertEquals("42", reenviada.getHeader(IdentityHeadersFilter.HEADER_USER_ID));
        assertEquals("CLIENTE", reenviada.getHeader(IdentityHeadersFilter.HEADER_USER_ROLE));
        assertEquals("juan.perez@mail.com", reenviada.getHeader(IdentityHeadersFilter.HEADER_USER_EMAIL));
    }

    @Test
    @DisplayName("Sin autenticación elimina por completo los headers de identidad")
    void eliminaIdentidadSinAutenticacion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(IdentityHeadersFilter.HEADER_USER_ID, "999");
        request.addHeader(IdentityHeadersFilter.HEADER_USER_ROLE, "ADMINISTRADOR");
        request.addHeader(IdentityHeadersFilter.HEADER_USER_EMAIL, "atacante@mail.com");

        HttpServletRequest reenviada = reenviar(request);

        assertNull(reenviada.getHeader(IdentityHeadersFilter.HEADER_USER_ID));
        assertNull(reenviada.getHeader(IdentityHeadersFilter.HEADER_USER_ROLE));
        assertNull(reenviada.getHeader(IdentityHeadersFilter.HEADER_USER_EMAIL));
    }

    private HttpServletRequest reenviar(MockHttpServletRequest request) throws Exception {
        AtomicReference<HttpServletRequest> reenviada = new AtomicReference<>();
        filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> reenviada.set((HttpServletRequest) req));
        return reenviada.get();
    }
}
