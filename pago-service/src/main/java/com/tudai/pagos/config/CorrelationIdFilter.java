package com.tudai.pagos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Trazabilidad: propaga el header {@code X-Correlation-Id} a los logs (MDC)
 * y a la respuesta. Si no viene, se genera uno. Nunca se registran datos
 * sensibles (contraseñas ni tokens).
 *
 * <p><b>EN-09</b> (criterios 2 y 4): cada solicitud lleva un identificador de
 * correlación, generado si no viene y propagado; dado un identificador se puede
 * reconstruir la operación a través de los servicios. Los logs no contienen
 * datos sensibles.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        long inicio = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            String usuario = request.getHeader("X-User-Id");
            log.info("{} {} | usuario={} | status={} | {}ms",
                    request.getMethod(), request.getRequestURI(),
                    usuario == null ? "-" : usuario,
                    response.getStatus(), System.currentTimeMillis() - inicio);
            MDC.remove(MDC_KEY);
        }
    }
}
