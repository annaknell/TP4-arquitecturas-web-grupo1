package org.example.apigatewayservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sanea la identidad que viaja hacia los microservicios.
 *
 * <p>Los headers {@code X-User-Id}, {@code X-User-Role} y {@code X-User-Email}
 * que envía el cliente se descartan siempre. Si la request está autenticada,
 * se reinyectan a partir de los claims del JWT validado por el gateway
 * ({@code userId}, {@code role} y {@code sub}).</p>
 *
 * <p>De este modo ningún cliente puede suplantar a otro usuario enviando
 * headers a mano, y los servicios internos siguen recibiendo la identidad
 * en el mismo formato que ya consumen.</p>
 */
public class IdentityHeadersFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdentityHeadersFilter.class);

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_USER_EMAIL = "X-User-Email";

    private static final Set<String> IDENTITY_HEADERS = Set.of(
            HEADER_USER_ID.toLowerCase(Locale.ROOT),
            HEADER_USER_ROLE.toLowerCase(Locale.ROOT),
            HEADER_USER_EMAIL.toLowerCase(Locale.ROOT)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Map<String, String> identity = new LinkedHashMap<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim != null) {
                userId = userIdClaim.toString();
                identity.put(HEADER_USER_ID, userId);
            }
            String role = jwt.getClaimAsString("role");
            if (role != null && !role.isBlank()) {
                identity.put(HEADER_USER_ROLE, role);
            }
            String email = jwt.getSubject();
            if (email != null && !email.isBlank()) {
                identity.put(HEADER_USER_EMAIL, email);
            }
        }

        long inicio = System.currentTimeMillis();
        try {
            filterChain.doFilter(new IdentityRequestWrapper(request, identity), response);
        } finally {
            // Log de acceso (US-38): el usuario es el del JWT validado, nunca el
            // header que pudo enviar el cliente. CorrelationIdFilter ya puso el
            // cid en el MDC y lo limpia después de este filtro.
            log.info("{} {} | usuario={} | status={} | {}ms",
                    request.getMethod(), request.getRequestURI(),
                    userId == null ? "-" : userId,
                    response.getStatus(), System.currentTimeMillis() - inicio);
        }
    }

    private static final class IdentityRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String> identity;

        private IdentityRequestWrapper(HttpServletRequest request, Map<String, String> identity) {
            super(request);
            this.identity = identity;
        }

        @Override
        public String getHeader(String name) {
            for (Map.Entry<String, String> entry : identity.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            return IDENTITY_HEADERS.contains(name.toLowerCase(Locale.ROOT)) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (!IDENTITY_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                return super.getHeaders(name);
            }
            String value = getHeader(name);
            return value == null
                    ? Collections.emptyEnumeration()
                    : Collections.enumeration(List.of(value));
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                String name = original.nextElement();
                if (!IDENTITY_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                    names.add(name);
                }
            }
            names.addAll(identity.keySet());
            return Collections.enumeration(names);
        }
    }
}
