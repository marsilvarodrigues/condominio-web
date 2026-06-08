package com.pmrodrigues.security.service;

import io.micrometer.core.annotation.Timed;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis-backed service that tracks revoked JWT IDs (blacklist) and maps refresh tokens to user
 * emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

  private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
  private static final String REFRESH_PREFIX = "jwt:refresh:";
  private static final String USER_REFRESH_PREFIX = "jwt:user_refresh:";

  private final StringRedisTemplate redisTemplate;

  /**
   * Adds a JWT's {@code jti} to the Redis blacklist with the given TTL; no-ops when {@code ttl} is
   * zero or negative.
   *
   * @param jti the JWT ID to revoke
   * @param ttl how long the entry should live in Redis (should match the token's remaining
   *     lifetime)
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

  /** Returns {@code true} if the given {@code jti} has been blacklisted in Redis. */
  @Timed(
      value = "token.blacklist.isBlacklisted",
      description = "Check if a JWT token is blacklisted")
  public boolean isBlacklisted(String jti) {
    log.info("Checking blacklist status for token with jti: {}", jti);
    boolean blacklisted =
        Optional.ofNullable(redisTemplate.hasKey(BLACKLIST_PREFIX + jti)).orElse(Boolean.FALSE);
    log.info("Token with jti: {} is blacklisted: {}", jti, blacklisted);
    return blacklisted;
  }

  /**
   * Persists bidirectional Redis mappings: {@code SHA-256(refreshToken) → email} and {@code email →
   * SHA-256(refreshToken)}, both expiring after {@code ttl}. Storing only the hash prevents raw
   * token exposure in Redis.
   *
   * @param ttl lifetime of the stored entries
   */
  @Timed(value = "token.blacklist.storeRefreshToken", description = "Store a refresh token")
  public void storeRefreshToken(String email, String refreshToken, Duration ttl) {
    log.info("Storing refresh token for user: {}", email);
    var hash = hashToken(refreshToken);
    redisTemplate.opsForValue().set(REFRESH_PREFIX + hash, email, ttl);
    redisTemplate.opsForValue().set(USER_REFRESH_PREFIX + email, hash, ttl);
    log.info("Refresh token stored successfully for user: {}", email);
  }

  /**
   * Looks up the email address associated with a refresh token by its SHA-256 hash.
   *
   * @return the email, or empty if the token is unknown or expired
   */
  @Timed(
      value = "token.blacklist.getEmailByRefreshToken",
      description = "Get email by refresh token")
  public Optional<String> getEmailByRefreshToken(String refreshToken) {
    log.info("Looking up email by refresh token");
    var email =
        Optional.ofNullable(
            redisTemplate.opsForValue().get(REFRESH_PREFIX + hashToken(refreshToken)));
    log.info("Email lookup result: {}", email.isPresent() ? "found" : "not found");
    return email;
  }

  /**
   * Removes both Redis refresh-token entries for the given user, effectively revoking the refresh
   * token.
   */
  @Timed(value = "token.blacklist.deleteRefreshToken", description = "Delete a refresh token")
  public void deleteRefreshToken(String email) {
    log.info("Deleting refresh token for user: {}", email);
    var storedHash = redisTemplate.opsForValue().get(USER_REFRESH_PREFIX + email);
    if (storedHash != null) {
      redisTemplate.delete(REFRESH_PREFIX + storedHash);
    }
    redisTemplate.delete(USER_REFRESH_PREFIX + email);
    log.info("Refresh token deleted for user: {}", email);
  }

  private String hashToken(String token) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
