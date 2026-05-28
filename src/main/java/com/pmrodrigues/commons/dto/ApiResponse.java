package com.pmrodrigues.commons.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard API envelope returned by all endpoints, carrying a correlation ID, payload, and server timestamp.
 *
 * @param <T> type of the response payload
 */
public record ApiResponse<T>(
        @JsonProperty("request_id") @Nullable UUID requestId,
        T data,
        Instant timestamp
) {
    /**
     * Creates an {@code ApiResponse} stamped with the current instant.
     *
     * @param <T> type of the response payload
     * @param requestId correlation UUID from the request, may be {@code null}
     * @param data payload to wrap
     * @return a new {@code ApiResponse}
     */
    public static <T> ApiResponse<T> of(@Nullable UUID requestId, T data) {
        return new ApiResponse<>(requestId, data, Instant.now());
    }
}