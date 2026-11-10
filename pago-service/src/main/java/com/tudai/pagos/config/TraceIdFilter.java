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
 * Asigna un identificador de correlación a cada request (EN-09):
 *  - usa X-Correlation-Id si llega (lo pone el gateway) y es válido; si no, genera uno;
 *  - lo deja en el MDC (aparece en todos los logs) y lo devuelve en la respuesta;
 *  - registra endpoint, método, usuario, código de respuesta y tiempo de procesamiento.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "traceId";

    private static final Logger log = LoggerFactory.getLogger("http.pagos");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String traceId = req.getHeader(HEADER);
        // se valida el formato para evitar inyección de saltos de línea en los logs
        if (traceId == null || !traceId.matches("[A-Za-z0-9._-]{1,100}")) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, traceId);
        res.setHeader(HEADER, traceId);
        long inicio = System.nanoTime();
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            String usuario = req.getHeader("X-User-Id");
            log.info("method={} endpoint={} user={} status={} ms={}",
                    req.getMethod(), req.getRequestURI(),
                    usuario == null ? "-" : usuario.replaceAll("[^A-Za-z0-9._-]", "?"),
                    res.getStatus(), ms);
            MDC.remove(MDC_KEY);
        }
    }
}
