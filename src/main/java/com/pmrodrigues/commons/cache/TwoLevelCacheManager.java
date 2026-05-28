package com.pmrodrigues.commons.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.pmrodrigues.commons.config.AppCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link CacheManager} that creates {@link TwoLevelCache} instances backed by a local Caffeine L1 and
 * a Redis-backed L2 manager.
 */
@RequiredArgsConstructor
public class TwoLevelCacheManager implements CacheManager {

    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    private final CacheManager l2Manager;
    private final AppCacheProperties properties;

    /**
     * Returns the {@link TwoLevelCache} for the given name, creating it on first access.
     *
     * @return a cache combining L1 (Caffeine) and L2 (Redis)
     */
    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, n ->
                new TwoLevelCache(n, buildL1(n), l2Manager.getCache(n)));
    }

    /**
     * Returns the names of all caches that have been created so far.
     *
     * @return unmodifiable view of created cache names
     */
    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(caches.keySet());
    }

    private Cache buildL1(String name) {
        var native$ = Caffeine.newBuilder()
                .expireAfterWrite(properties.l1Ttl(name))
                .maximumSize(properties.maxSize(name))
                .build();
        return new CaffeineCache(name, native$);
    }
}