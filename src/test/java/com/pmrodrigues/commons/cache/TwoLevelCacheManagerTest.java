package com.pmrodrigues.commons.cache;

import com.pmrodrigues.commons.config.AppCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoLevelCacheManagerTest {

    @Mock CacheManager l2Manager;
    @Mock Cache l2Cache;

    AppCacheProperties properties;
    TwoLevelCacheManager manager;

    @BeforeEach
    void setUp() {
        properties = new AppCacheProperties();
        manager = new TwoLevelCacheManager(l2Manager, properties);
    }

    @Test
    void getCache_returnsTwoLevelCacheInstance() {
        when(l2Manager.getCache("items")).thenReturn(l2Cache);

        var cache = manager.getCache("items");

        assertThat(cache).isInstanceOf(TwoLevelCache.class);
        assertThat(cache.getName()).isEqualTo("items");
    }

    @Test
    void getCache_returnsSameInstanceOnRepeatedCalls() {
        when(l2Manager.getCache("items")).thenReturn(l2Cache);

        var first  = manager.getCache("items");
        var second = manager.getCache("items");

        assertThat(first).isSameAs(second);
        verify(l2Manager, times(1)).getCache("items");
    }

    @Test
    void getCache_differentNamesProduceDifferentInstances() {
        when(l2Manager.getCache("items")).thenReturn(l2Cache);
        when(l2Manager.getCache("orders")).thenReturn(mock(Cache.class));

        var items  = manager.getCache("items");
        var orders = manager.getCache("orders");

        assertThat(items).isNotSameAs(orders);
        assertThat(items.getName()).isEqualTo("items");
        assertThat(orders.getName()).isEqualTo("orders");
    }

    @Test
    void getCacheNames_reflectsCreatedCaches() {
        when(l2Manager.getCache(anyString())).thenReturn(l2Cache);

        manager.getCache("a");
        manager.getCache("b");

        assertThat(manager.getCacheNames()).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void getCache_usesPerCacheL1TtlFromProperties() {
        var spec = new AppCacheProperties.CacheSpec();
        spec.setL1Ttl(Duration.ofMinutes(30));
        properties.getCaches().put("items", spec);
        when(l2Manager.getCache("items")).thenReturn(l2Cache);

        // Creating the cache should succeed with the custom TTL
        var cache = manager.getCache("items");

        assertThat(cache).isNotNull();
    }

    @Test
    void getCache_usesDefaultTtlWhenNoCacheSpecConfigured() {
        properties.setL1DefaultTtl(Duration.ofMinutes(15));
        when(l2Manager.getCache("items")).thenReturn(l2Cache);

        var cache = manager.getCache("items");

        assertThat(cache).isNotNull();
    }
}