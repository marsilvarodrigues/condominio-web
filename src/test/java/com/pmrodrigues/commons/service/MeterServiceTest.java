package com.pmrodrigues.commons.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeterServiceTest {

    SimpleMeterRegistry registry;
    MeterService meterService;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        meterService = new MeterService(registry);
    }

    @Test
    void incrementError_registersCounterWithCorrectTags() {
        meterService.incrementError("auth.errors", "invalid_credentials", "Auth errors", 401);

        Counter counter = registry.find("auth.errors")
                .tag("error_type", "invalid_credentials")
                .tag("http_status", "401")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void incrementError_accumulatesOnRepeatedCalls() {
        meterService.incrementError("auth.errors", "invalid_credentials", "Auth errors", 401);
        meterService.incrementError("auth.errors", "invalid_credentials", "Auth errors", 401);
        meterService.incrementError("auth.errors", "invalid_credentials", "Auth errors", 401);

        double count = registry.find("auth.errors")
                .tag("error_type", "invalid_credentials")
                .tag("http_status", "401")
                .counter()
                .count();

        assertThat(count).isEqualTo(3.0);
    }

    @Test
    void incrementError_differentTagsProduceDifferentCounters() {
        meterService.incrementError("auth.errors", "invalid_credentials", "Auth errors", 401);
        meterService.incrementError("auth.errors", "validation_error", "Auth errors", 400);

        double credentials = registry.find("auth.errors")
                .tag("error_type", "invalid_credentials")
                .tag("http_status", "401")
                .counter().count();

        double validation = registry.find("auth.errors")
                .tag("error_type", "validation_error")
                .tag("http_status", "400")
                .counter().count();

        assertThat(credentials).isEqualTo(1.0);
        assertThat(validation).isEqualTo(1.0);
    }

    @Test
    void incrementError_setsDescriptionOnCounter() {
        meterService.incrementError("auth.errors", "account_unavailable", "My description", 401);

        Counter counter = registry.find("auth.errors")
                .tag("error_type", "account_unavailable")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.getId().getDescription()).isEqualTo("My description");
    }

    @Test
    void incrementError_worksWithDifferentMeterNames() {
        meterService.incrementError("business.errors", "not_found", "Business errors", 404);

        Counter counter = registry.find("business.errors")
                .tag("error_type", "not_found")
                .tag("http_status", "404")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}