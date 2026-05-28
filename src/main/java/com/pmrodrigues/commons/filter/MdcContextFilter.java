package com.pmrodrigues.commons.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates the MDC with correlation, request, and idempotency IDs for structured logging.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcContextFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER  = "X-Correlation-ID";
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    public static final String REQUEST_ID_HEADER      = "X-Request-ID";

    private static final String MDC_CORRELATION_ID  = "correlation_id";
    private static final String MDC_REQUEST_ID      = "request_id";
    private static final String MDC_IDEMPOTENCY_ID  = "idempotency_id";
    private static final String MDC_HTTP_METHOD     = "http_method";
    private static final String MDC_HTTP_URL        = "http_url";
    private static final String MDC_QUERY_PARAMS    = "query_params";

    /**
     * Populates the MDC, logs request entry/exit with duration, and ensures the MDC is cleared after the response.
     *
     * @throws ServletException if the filter chain raises a servlet error
     * @throws IOException      if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            populateMdc(request);
            response.setHeader(CORRELATION_ID_HEADER, MDC.get(MDC_CORRELATION_ID));
            log.info(">> {} {}", request.getMethod(), fullUrl(request));
            chain.doFilter(request, response);
        } finally {
            log.info("<< {} {} status={} duration={}ms",
                    request.getMethod(), fullUrl(request),
                    response.getStatus(), System.currentTimeMillis() - start);
            MDC.clear();
        }
    }

    private void populateMdc(HttpServletRequest request) {
        String correlationId = header(request, CORRELATION_ID_HEADER);
        MDC.put(MDC_CORRELATION_ID, correlationId != null ? correlationId : UUID.randomUUID().toString());

        String requestId = header(request, REQUEST_ID_HEADER);
        if (requestId != null) {
            MDC.put(MDC_REQUEST_ID, requestId);
        }

        String idempotencyKey = header(request, IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey != null) {
            MDC.put(MDC_IDEMPOTENCY_ID, idempotencyKey);
        }

        MDC.put(MDC_HTTP_METHOD, request.getMethod());
        MDC.put(MDC_HTTP_URL, request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null) {
            MDC.put(MDC_QUERY_PARAMS, queryString);
        }
    }

    private static String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return (value != null && !value.isBlank()) ? value : null;
    }

    private static String fullUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return query != null ? uri + "?" + query : uri;
    }
}