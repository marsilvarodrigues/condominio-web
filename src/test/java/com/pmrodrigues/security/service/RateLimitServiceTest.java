package com.pmrodrigues.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate);
        ReflectionTestUtils.setField(rateLimitService, "loginMaxRequests", 5);
        ReflectionTestUtils.setField(rateLimitService, "loginWindowSeconds", 60);
        ReflectionTestUtils.setField(rateLimitService, "refreshMaxRequests", 10);
        ReflectionTestUtils.setField(rateLimitService, "refreshWindowSeconds", 60);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void checkLoginRateLimit_belowLimit_doesNotThrow() {
        when(valueOperations.increment(any())).thenReturn(1L);

        assertThatCode(() -> rateLimitService.checkLoginRateLimit("10.0.0.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkLoginRateLimit_firstRequest_setsExpiry() {
        when(valueOperations.increment(any())).thenReturn(1L);

        rateLimitService.checkLoginRateLimit("10.0.0.1");

        verify(redisTemplate).expire(eq("rate:login:10.0.0.1"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void checkLoginRateLimit_subsequentRequests_doesNotResetExpiry() {
        when(valueOperations.increment(any())).thenReturn(3L);

        rateLimitService.checkLoginRateLimit("10.0.0.1");

        verify(redisTemplate, never()).expire(any(), any());
    }

    @Test
    void checkLoginRateLimit_atLimit_doesNotThrow() {
        when(valueOperations.increment(any())).thenReturn(5L);

        assertThatCode(() -> rateLimitService.checkLoginRateLimit("10.0.0.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkLoginRateLimit_exceedsLimit_throwsTooManyRequests() {
        when(valueOperations.increment(any())).thenReturn(6L);

        assertThatThrownBy(() -> rateLimitService.checkLoginRateLimit("10.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(TOO_MANY_REQUESTS.value());
    }

    @Test
    void checkRefreshRateLimit_belowLimit_doesNotThrow() {
        when(valueOperations.increment(any())).thenReturn(5L);

        assertThatCode(() -> rateLimitService.checkRefreshRateLimit("10.0.0.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkRefreshRateLimit_exceedsLimit_throwsTooManyRequests() {
        when(valueOperations.increment(any())).thenReturn(11L);

        assertThatThrownBy(() -> rateLimitService.checkRefreshRateLimit("10.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(TOO_MANY_REQUESTS.value());
    }
}
