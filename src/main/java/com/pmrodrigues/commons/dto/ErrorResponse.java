package com.pmrodrigues.commons.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Standard error envelope returned by exception handlers, with an optional per-field validation map.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String error, String message, Map<String, String> fields) {

    /**
     * Creates an {@code ErrorResponse} without field-level detail, suitable for non-validation errors.
     *
     * @param error short error code or HTTP reason phrase
     * @param message human-readable description
     */
    public ErrorResponse(String error, String message) {
        this(error, message, null);
    }
}