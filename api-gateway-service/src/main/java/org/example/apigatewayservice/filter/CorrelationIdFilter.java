package org.example.apigatewayservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || !VALID_ID.matcher(correlationId).matches()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);

        try {
            filterChain.doFilter(new CorrelationRequestWrapper(request, correlationId), response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static final class CorrelationRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> headers = new LinkedHashMap<>();

        private CorrelationRequestWrapper(HttpServletRequest request, String correlationId) {
            super(request);
            headers.put(HEADER, correlationId);
        }

        @Override
        public String getHeader(String name) {
            return HEADER.equalsIgnoreCase(name) ? headers.get(HEADER) : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return HEADER.equalsIgnoreCase(name)
                ? Collections.enumeration(List.of(headers.get(HEADER)))
                : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            if (names.stream().noneMatch(HEADER::equalsIgnoreCase)) {
                names.add(HEADER);
            }
            return Collections.enumeration(names);
        }
    }
}