package com.pmrodrigues.security.controller;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.dto.ErrorResponse;
import com.pmrodrigues.commons.service.MeterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;
import static org.springframework.http.HttpStatus.*;

/**
 * Global exception handler for authentication and authorization failures, returning standardized {@link ApiResponse} error payloads and recording metrics.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class AuthExceptionHandler {

    private static final String METRIC_NAME = "auth.errors";
    private static final String AUTHENTICATION_ERROR_DESCRIPTION = "Authentication and authorization error occurrences";

    private final MeterService meterService;

    /**
     * Handles invalid credentials by returning a {@code 401} response with an {@code invalid_credentials} error code.
     *
     * @return {@code 401} with error details
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.info("Handling BadCredentialsException for request: {}", request.getRequestURI());
        log.error("Authentication failed - invalid credentials: {}", ex.getMessage());
        var response = errorResponse(request, UNAUTHORIZED, "invalid_credentials", "Invalid email or password");
        log.info("Returning 401 response for invalid credentials");
        return response;
    }

    /**
     * Handles disabled or locked accounts by returning a {@code 401} response with an {@code account_unavailable} error code.
     *
     * @return {@code 401} with a message indicating whether the account is disabled or locked
     */
    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDisabledOrLocked(AuthenticationException ex, HttpServletRequest request) {
        log.info("Handling {} for request: {}", ex.getClass().getSimpleName(), request.getRequestURI());
        log.error("Authentication failed - account unavailable: {}", ex.getMessage());
        String message = ex instanceof DisabledException ? "Account is disabled" : "Account is locked";
        var response = errorResponse(request, UNAUTHORIZED, "account_unavailable", message);
        log.info("Returning 401 response for account unavailable");
        return response;
    }

    /**
     * Catch-all handler for any other {@link AuthenticationException}, returning a generic {@code 401} response.
     *
     * @return {@code 401} with an {@code authentication_failed} error code
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.info("Handling AuthenticationException for request: {}", request.getRequestURI());
        log.error("Authentication failed: {}", ex.getMessage());
        var response = errorResponse(request, UNAUTHORIZED, "authentication_failed", "Authentication failed");
        log.info("Returning 401 response for authentication failure");
        return response;
    }

    /**
     * Handles {@link ResponseStatusException} by forwarding its status code and reason phrase in the API response.
     *
     * @return response with the exception's HTTP status and reason
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        log.info("Handling ResponseStatusException for request: {}", request.getRequestURI());
        log.error("Response status error - status: {}, reason: {}", ex.getStatusCode(), ex.getReason());
        int status = ex.getStatusCode().value();
        meterService.incrementError(METRIC_NAME, "response_status_error", AUTHENTICATION_ERROR_DESCRIPTION, status);
        var error = new ErrorResponse("error", ex.getReason());
        var response = ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.of(requestId(request), error));
        log.info("Returning {} response for response status error", status);
        return response;
    }

    /**
     * Handles bean-validation failures by collecting per-field error messages and returning a {@code 400} response.
     *
     * @return {@code 400} with a map of field names to validation messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.info("Handling MethodArgumentNotValidException for request: {}", request.getRequestURI());
        log.error("Validation failed with {} field error(s)", ex.getBindingResult().getFieldErrorCount());
        meterService.incrementError(METRIC_NAME, "validation_error", AUTHENTICATION_ERROR_DESCRIPTION, BAD_REQUEST.value());
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (first, second) -> first
                ));
        var error = new ErrorResponse("validation_error", "Invalid request fields", fields);
        var response = ResponseEntity.status(BAD_REQUEST).body(ApiResponse.of(requestId(request), error));
        log.info("Returning 400 response for validation error");
        return response;
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> errorResponse(
            HttpServletRequest request, HttpStatusCode status, String errorType, String message) {
        meterService.incrementError(METRIC_NAME, errorType, AUTHENTICATION_ERROR_DESCRIPTION, status.value());
        return ResponseEntity.status(status).body(ApiResponse.of(requestId(request), new ErrorResponse(errorType, message)));
    }
}
