package com.pmrodrigues.commons.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around Micrometer for incrementing tagged error counters without exposing {@link MeterRegistry} directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeterService {

    private static final String ERROR_TYPE = "error_type";
    private static final String HTTP_STATUS = "http_status";

    private final MeterRegistry meterRegistry;

    /**
     * Registers (if not already registered) and increments a counter with {@code error_type} and {@code http_status} tags.
     *
     * @param meterName   Micrometer counter name
     * @param errorType   value for the {@code error_type} tag
     * @param description human-readable description of the counter
     * @param httpStatus  HTTP status code recorded as the {@code http_status} tag
     */
    @Timed(value = "meter.service.incrementError", description = "Increment error counter")
    public void incrementError(String meterName, String errorType, String description, int httpStatus) {
        log.info("Incrementing error counter - meter: {}, errorType: {}, httpStatus: {}", meterName, errorType, httpStatus);
        Counter.builder(meterName)
                .tag(ERROR_TYPE, errorType)
                .tag(HTTP_STATUS, String.valueOf(httpStatus))
                .description(description)
                .register(meterRegistry)
                .increment();
        log.info("Error counter incremented successfully - meter: {}", meterName);
    }
}