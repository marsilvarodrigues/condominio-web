package com.pmrodrigues.commons.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * Spring {@link Cache} implementation that combines a local Caffeine (L1) and a distributed Redis (L2) cache.
 */
@RequiredArgsConstructor
public class TwoLevelCache implements Cache {

    private final String name;
    private final Cache l1;
    private final Cache l2;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    /**
     * L1 → L2 → null. On L2 hit, warms L1 before returning.
     */
    @Override
    public ValueWrapper get(Object key) {
        var hit = l1.get(key);
        if (hit != null) return hit;

        hit = l2.get(key);
        if (hit != null) {
            l1.put(key, hit.get());
        }
        return hit;
    }

    /**
     * Returns the cached value cast to {@code type}, or {@code null} if absent.
     *
     * @param <T> expected value type
     * @param type class to cast the cached value to
     * @return the cached value, or {@code null}
     */
    @Override
    public <T> T get(Object key, Class<T> type) {
        var wrapper = get(key);
        return wrapper != null ? type.cast(wrapper.get()) : null;
    }

    /**
     * Returns the cached value, loading and storing it via {@code valueLoader} on a miss.
     *
     * @param <T> expected value type
     * @param valueLoader supplier called when the key is absent from both cache levels
     * @return the cached or freshly loaded value
     * @throws ValueRetrievalException if {@code valueLoader} throws
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        var wrapper = get(key);
        if (wrapper != null) return (T) wrapper.get();
        try {
            var value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    /**
     * Writes {@code value} to both L1 and L2 caches.
     */
    @Override
    public void put(Object key, Object value) {
        l1.put(key, value);
        l2.put(key, value);
    }

    /**
     * Removes the entry for {@code key} from both L1 and L2 caches.
     */
    @Override
    public void evict(Object key) {
        l1.evict(key);
        l2.evict(key);
    }

    /**
     * Removes all entries from both L1 and L2 caches.
     */
    @Override
    public void clear() {
        l1.clear();
        l2.clear();
    }
}