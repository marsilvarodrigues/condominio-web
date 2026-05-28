package com.pmrodrigues.commons.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * HandlerInterceptor that validates the {@code X-Request-ID} header as a UUID and stores it as a request attribute.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestIdInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    private final ObjectMapper objectMapper;

    /**
     * Rejects requests whose {@code X-Request-ID} header is absent or not a valid UUID with a 400 response.
     *
     * @return {@code true} if the header is valid and processing should continue; {@code false} if rejected
     * @throws Exception if writing the error response fails
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("Validating X-Request-ID header for request: {} {}", request.getMethod(), request.getRequestURI());

        String header = request.getHeader(REQUEST_ID_HEADER);

        if (header == null || header.isBlank()) {
            log.info("X-Request-ID header is missing or blank, returning 400");
            writeBadRequest(response, null, "missing_request_id", "Header X-Request-ID is required");
            return false;
        }

        try {
            UUID requestId = UUID.fromString(header);
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
            log.info("X-Request-ID validated successfully: {}", requestId);
            return true;
        } catch (IllegalArgumentException e) {
            log.error("Invalid X-Request-ID format: '{}' - {}", header, e.getMessage());
            writeBadRequest(response, null, "invalid_request_id", "Header X-Request-ID must be a valid UUID");
            return false;
        }
    }

    private void writeBadRequest(HttpServletResponse response, UUID requestId, String error, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.of(requestId, new ErrorResponse(error, message))
        );
    }
}