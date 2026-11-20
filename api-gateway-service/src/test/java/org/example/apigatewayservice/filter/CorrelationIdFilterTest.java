package org.example.apigatewayservice.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void conservaElCorrelationIdEntranteAlReenviar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> reenviado = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> reenviado.set(
                ((jakarta.servlet.http.HttpServletRequest) req).getHeader(CorrelationIdFilter.HEADER)));

        assertEquals("corr-123", reenviado.get());
        assertEquals("corr-123", response.getHeader(CorrelationIdFilter.HEADER));
    }

    @Test
    void generaYReenviaElCorrelationIdCuandoFalta() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> reenviado = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> reenviado.set(
                ((jakarta.servlet.http.HttpServletRequest) req).getHeader(CorrelationIdFilter.HEADER)));

        assertNotNull(reenviado.get());
        assertEquals(reenviado.get(), response.getHeader(CorrelationIdFilter.HEADER));
    }
}
