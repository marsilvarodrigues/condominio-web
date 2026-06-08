package com.pmrodrigues.security.service;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import io.micrometer.core.annotation.Timed;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Redis-backed rate limiter for authentication endpoints. Counters are keyed by endpoint + client
 * IP and expire after the configured window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

  private final StringRedisTemplate redisTemplate;

  @Value("${app.security.rate-limit.login.max-requests:5}")
  private int loginMaxRequests;

  @Value("${app.security.rate-limit.login.window-seconds:60}")
  private int loginWindowSeconds;

  @Value("${app.security.rate-limit.refresh.max-requests:10}")
  private int refreshMaxRequests;

  @Value("${app.security.rate-limit.refresh.window-seconds:60}")
  private int refreshWindowSeconds;

  /**
   * Enforces the login rate limit for the given client IP.
   *
   * @param clientIp the resolved client IP address
   * @throws ResponseStatusException 429 if the limit is exceeded
   */
  @Timed(value = "rate.limit.login", description = "Check login rate limit")
  public void checkLoginRateLimit(String clientIp) {
    log.info("Checking login rate limit for IP: {}", clientIp);
    enforce("login:" + clientIp, loginMaxRequests, Duration.ofSeconds(loginWindowSeconds));
  }

  /**
   * Enforces the token-refresh rate limit for the given client IP.
   *
   * @param clientIp the resolved client IP address
   * @throws ResponseStatusException 429 if the limit is exceeded
   */
  @Timed(value = "rate.limit.refresh", description = "Check refresh rate limit")
  public void checkRefreshRateLimit(String clientIp) {
    log.info("Checking refresh rate limit for IP: {}", clientIp);
    enforce("refresh:" + clientIp, refreshMaxRequests, Duration.ofSeconds(refreshWindowSeconds));
  }

  private void enforce(String key, int maxRequests, Duration window) {
    String redisKey = "rate:" + key;
    Long count = redisTemplate.opsForValue().increment(redisKey);
    if (count != null && count == 1L) {
      redisTemplate.expire(redisKey, window);
    }
    if (count != null && count > maxRequests) {
      log.error("Rate limit exceeded for key: {}", key);
      throw new ResponseStatusException(TOO_MANY_REQUESTS, "Too many requests. Try again later.");
    }
  }
}
