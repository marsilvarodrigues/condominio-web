package com.pmrodrigues.security.service;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed service that tracks revoked JWT IDs (blacklist) and maps refresh tokens to user emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX    = "jwt:blacklist:";
    private static final String REFRESH_PREFIX      = "jwt:refresh:";
    private static final String USER_REFRESH_PREFIX = "jwt:user_refresh:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Adds a JWT's {@code jti} to the Redis blacklist with the given TTL; no-ops when {@code ttl} is zero or negative.
     *
     * @param jti the JWT ID to revoke
     * @param ttl how long the entry should live in Redis (should match the token's remaining lifetime)
     */
    @Timed(value = "token.blacklist.blacklistToken", description = "Blacklist a JWT token")
    public void blacklistToken(String jti, Duration ttl) {
        log.info("Blacklisting token with jti: {}", jti);
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", ttl);
            log.info("Token blacklisted successfully with jti: {} for {} seconds", jti, ttl.getSeconds());
        } else {
            log.info("Token not blacklisted - TTL is zero or negative for jti: {}", jti);
        }
    }

    /**
     * Returns {@code true} if the given {@code jti} has been blacklisted in Redis.
     */
    @Timed(value = "token.blacklist.isBlacklisted", description = "Check if a JWT token is blacklisted")
    public boolean isBlacklisted(String jti) {
        log.info("Checking blacklist status for token with jti: {}", jti);
        boolean blacklisted = Optional.ofNullable(redisTemplate.hasKey(BLACKLIST_PREFIX + jti))
                .orElse(Boolean.FALSE);
        log.info("Token with jti: {} is blacklisted: {}", jti, blacklisted);
        return blacklisted;
    }

    /**
     * Persists bidirectional Redis mappings: {@code refreshToken → email} and {@code email → refreshToken}, both expiring after {@code ttl}.
     *
     * @param ttl lifetime of the stored entries
     */
    @Timed(value = "token.blacklist.storeRefreshToken", description = "Store a refresh token")
    public void storeRefreshToken(String email, String refreshToken, Duration ttl) {
        log.info("Storing refresh token for user: {}", email);
        redisTemplate.opsForValue().set(REFRESH_PREFIX + refreshToken, email, ttl);
        redisTemplate.opsForValue().set(USER_REFRESH_PREFIX + email, refreshToken, ttl);
        log.info("Refresh token stored successfully for user: {}", email);
    }

    /**
     * Looks up the email address associated with a refresh token.
     *
     * @return the email, or empty if the token is unknown or expired
     */
    @Timed(value = "token.blacklist.getEmailByRefreshToken", description = "Get email by refresh token")
    public Optional<String> getEmailByRefreshToken(String refreshToken) {
        log.info("Looking up email by refresh token");
        var email = Optional.ofNullable(redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshToken));
        log.info("Email lookup result: {}", email.isPresent() ? "found" : "not found");
        return email;
    }

    /**
     * Removes both Redis refresh-token entries for the given user, effectively revoking the refresh token.
     */
    @Timed(value = "token.blacklist.deleteRefreshToken", description = "Delete a refresh token")
    public void deleteRefreshToken(String email) {
        log.info("Deleting refresh token for user: {}", email);
        var refreshToken = redisTemplate.opsForValue().get(USER_REFRESH_PREFIX + email);
        if (refreshToken != null) {
            redisTemplate.delete(REFRESH_PREFIX + refreshToken);
        }
        redisTemplate.delete(USER_REFRESH_PREFIX + email);
        log.info("Refresh token deleted for user: {}", email);
    }
}