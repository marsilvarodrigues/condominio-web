package com.pmrodrigues.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenBlacklistServiceTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @InjectMocks
    TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void blacklistToken_withPositiveTtl_storesInRedis() {
        service.blacklistToken("jti-1", Duration.ofMinutes(5));

        verify(valueOperations).set("jwt:blacklist:jti-1", "1", Duration.ofMinutes(5));
    }

    @Test
    void blacklistToken_withNegativeTtl_doesNotStoreInRedis() {
        service.blacklistToken("jti-1", Duration.ofMinutes(-1));

        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void blacklistToken_withZeroTtl_doesNotStoreInRedis() {
        service.blacklistToken("jti-1", Duration.ZERO);

        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void isBlacklisted_whenKeyExists_returnsTrue() {
        when(redisTemplate.hasKey("jwt:blacklist:jti-1")).thenReturn(Boolean.TRUE);

        assertThat(service.isBlacklisted("jti-1")).isTrue();
    }

    @Test
    void isBlacklisted_whenKeyDoesNotExist_returnsFalse() {
        when(redisTemplate.hasKey("jwt:blacklist:jti-1")).thenReturn(Boolean.FALSE);

        assertThat(service.isBlacklisted("jti-1")).isFalse();
    }

    @Test
    void isBlacklisted_whenRedisReturnsNull_returnsFalse() {
        when(redisTemplate.hasKey("jwt:blacklist:jti-1")).thenReturn(null);

        assertThat(service.isBlacklisted("jti-1")).isFalse();
    }

    @Test
    void storeRefreshToken_storesBothEntries() {
        service.storeRefreshToken("user@test.com", "refresh-uuid", Duration.ofDays(1));

        verify(valueOperations).set("jwt:refresh:refresh-uuid", "user@test.com", Duration.ofDays(1));
        verify(valueOperations).set("jwt:user_refresh:user@test.com", "refresh-uuid", Duration.ofDays(1));
    }

    @Test
    void getEmailByRefreshToken_whenExists_returnsEmail() {
        when(valueOperations.get("jwt:refresh:token-123")).thenReturn("user@test.com");

        assertThat(service.getEmailByRefreshToken("token-123")).contains("user@test.com");
    }

    @Test
    void getEmailByRefreshToken_whenNotExists_returnsEmpty() {
        when(valueOperations.get("jwt:refresh:token-123")).thenReturn(null);

        assertThat(service.getEmailByRefreshToken("token-123")).isEmpty();
    }

    @Test
    void deleteRefreshToken_whenRefreshTokenExists_deletesBothKeys() {
        when(valueOperations.get("jwt:user_refresh:user@test.com")).thenReturn("refresh-uuid");

        service.deleteRefreshToken("user@test.com");

        verify(redisTemplate).delete("jwt:refresh:refresh-uuid");
        verify(redisTemplate).delete("jwt:user_refresh:user@test.com");
    }

    @Test
    void deleteRefreshToken_whenNoRefreshToken_deletesOnlyUserKey() {
        when(valueOperations.get("jwt:user_refresh:user@test.com")).thenReturn(null);

        service.deleteRefreshToken("user@test.com");

        verify(redisTemplate, times(1)).delete(any(String.class));
        verify(redisTemplate).delete("jwt:user_refresh:user@test.com");
    }
}
