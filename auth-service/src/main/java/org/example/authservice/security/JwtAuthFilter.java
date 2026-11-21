package org.example.authservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.authservice.service.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Valida el token Bearer de cada request, arma el SecurityContext y expone
 * {@code userId} / {@code role} como atributos (misma semántica que los
 * headers X-User-Id / X-User-Role de US-44 que propaga el API Gateway).
 *
 * <p><b>US-45</b> (criterio 3): verificación de firma y vencimiento del token.
 * <b>Criterio transversal US-46 a US-49</b>: "un token ausente, inválido o
 * vencido devuelve 401" — ante cualquier error de validación el filtro deja
 * la request sin autenticar y el EntryPoint responde 401.</p>
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "X-User-Id";
    public static final String ATTR_USER_ROLE = "X-User-Role";

    private final JwtDecoder jwtDecoder;

    public JwtAuthFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            try {
                Jwt jwt = jwtDecoder.decode(token);
                Object userIdClaim = jwt.getClaim(JwtService.CLAIM_USER_ID);
                String role = jwt.getClaimAsString(JwtService.CLAIM_ROLE);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                jwt.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                authentication.setDetails(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (userIdClaim != null) {
                    request.setAttribute(ATTR_USER_ID, Long.valueOf(userIdClaim.toString()));
                }
                request.setAttribute(ATTR_USER_ROLE, role);
            } catch (JwtException | NumberFormatException ex) {
                // Token inválido, expirado o con un claim userId no numérico (p. ej. un UUID
                // emitido antes de la migración a Long): no se autentica y el EntryPoint
                // responde 401 en lugar de propagar un 500.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
