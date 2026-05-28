package com.pmrodrigues.commons.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppCachePropertiesTest {

    AppCacheProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AppCacheProperties();
    }

    // ── defaults ──────────────────────────────────────────────────────────

    @Test
    void l1Ttl_whenNoCacheSpec_returnsL1Default() {
        properties.setL1DefaultTtl(Duration.ofMinutes(10));

        assertThat(properties.l1Ttl("unknown")).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void l2Ttl_whenNoCacheSpec_returnsL2Default() {
        properties.setL2DefaultTtl(Duration.ofHours(1));

        assertThat(properties.l2Ttl("unknown")).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void maxSize_whenNoCacheSpec_returnsL1DefaultMaxSize() {
        properties.setL1DefaultMaxSize(500);

        assertThat(properties.maxSize("unknown")).isEqualTo(500);
    }

    // ── per-cache overrides ───────────────────────────────────────────────

    @Test
    void l1Ttl_whenCacheSpecDefinesL1Ttl_returnsOverride() {
        var spec = specWith(Duration.ofMinutes(5), null, null);
        properties.setCaches(Map.of("orders", spec));

        assertThat(properties.l1Ttl("orders")).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void l2Ttl_whenCacheSpecDefinesL2Ttl_returnsOverride() {
        var spec = specWith(null, Duration.ofHours(2), null);
        properties.setCaches(Map.of("orders", spec));

        assertThat(properties.l2Ttl("orders")).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void maxSize_whenCacheSpecDefinesMaxSize_returnsOverride() {
        var spec = specWith(null, null, 100L);
        properties.setCaches(Map.of("orders", spec));

        assertThat(properties.maxSize("orders")).isEqualTo(100L);
    }

    // ── partial spec falls back to default ────────────────────────────────

    @Test
    void l1Ttl_whenCacheSpecExistsButL1TtlIsNull_returnsDefault() {
        properties.setL1DefaultTtl(Duration.ofMinutes(10));
        var spec = specWith(null, Duration.ofHours(2), null); // only L2 configured
        properties.setCaches(Map.of("orders", spec));

        assertThat(properties.l1Ttl("orders")).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void l2Ttl_whenCacheSpecExistsButL2TtlIsNull_returnsDefault() {
        properties.setL2DefaultTtl(Duration.ofHours(1));
        var spec = specWith(Duration.ofMinutes(5), null, null); // only L1 configured
        properties.setCaches(Map.of("orders", spec));

        assertThat(properties.l2Ttl("orders")).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void maxSize_whenCacheSpecExistsButMaxSizeIsNull_returnsDefault() {
        properties.setL1DefaultMaxSize(300);
        var spec = specWith(Duration.ofMinutes(5), null, null);
        properties.setCaches(Map.of("orders", spec));

        assertThat(properties.maxSize("orders")).isEqualTo(300);
    }

    // ── helper ────────────────────────────────────────────────────────────

    private AppCacheProperties.CacheSpec specWith(Duration l1Ttl, Duration l2Ttl, Long maxSize) {
        var spec = new AppCacheProperties.CacheSpec();
        spec.setL1Ttl(l1Ttl);
        spec.setL2Ttl(l2Ttl);
        spec.setMaxSize(maxSize);
        return spec;
    }
}