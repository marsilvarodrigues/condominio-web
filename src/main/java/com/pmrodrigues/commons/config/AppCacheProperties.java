package com.pmrodrigues.commons.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for the two-level cache, bound to the {@code app.cache} prefix. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {

  private Duration l1DefaultTtl = Duration.ofMinutes(10);
  private Duration l2DefaultTtl = Duration.ofHours(1);
  private long l1DefaultMaxSize = 500;
  private Map<String, CacheSpec> caches = new LinkedHashMap<>();

  /**
   * Returns the L1 TTL for the named cache, falling back to the default.
   *
   * @param name cache name
   * @return effective L1 TTL
   */
  public Duration l1Ttl(String name) {
    var spec = caches.get(name);
    return (spec != null && spec.getL1Ttl() != null) ? spec.getL1Ttl() : l1DefaultTtl;
  }

  /**
   * Returns the L2 TTL for the named cache, falling back to the default.
   *
   * @param name cache name
   * @return effective L2 TTL
   */
  public Duration l2Ttl(String name) {
    var spec = caches.get(name);
    return (spec != null && spec.getL2Ttl() != null) ? spec.getL2Ttl() : l2DefaultTtl;
  }

  /**
   * Returns the maximum L1 entry count for the named cache, falling back to the default.
   *
   * @param name cache name
   * @return effective maximum size
   */
  public long maxSize(String name) {
    var spec = caches.get(name);
    return (spec != null && spec.getMaxSize() != null) ? spec.getMaxSize() : l1DefaultMaxSize;
  }

  /** Per-cache TTL and size overrides. */
  @Getter
  @Setter
  public static class CacheSpec {
    private Duration l1Ttl;
    private Duration l2Ttl;
    private Long maxSize;
  }
}
