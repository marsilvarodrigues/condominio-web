package com.pmrodrigues.commons.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;

import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoLevelCacheTest {

    @Mock Cache l1;
    @Mock Cache l2;

    TwoLevelCache cache;

    @BeforeEach
    void setUp() {
        cache = new TwoLevelCache("items", l1, l2);
    }

    // ── metadata ──────────────────────────────────────────────────────────

    @Test
    void getName_returnsConfiguredName() {
        assertThat(cache.getName()).isEqualTo("items");
    }

    @Test
    void getNativeCache_returnsSelf() {
        assertThat(cache.getNativeCache()).isSameAs(cache);
    }

    // ── get(key) — L1 hit ─────────────────────────────────────────────────

    @Test
    void get_whenL1Hit_returnsL1ValueAndSkipsL2() {
        ValueWrapper wrapper = () -> "cached";
        when(l1.get("k")).thenReturn(wrapper);

        assertThat(cache.get("k")).isSameAs(wrapper);
        verifyNoInteractions(l2);
    }

    @Test
    void get_whenL1HitWithNullValue_returnsWrapperAndSkipsL2() {
        ValueWrapper nullWrapper = () -> null;
        when(l1.get("k")).thenReturn(nullWrapper);

        assertThat(cache.get("k")).isSameAs(nullWrapper);
        verifyNoInteractions(l2);
    }

    // ── get(key) — L2 hit ─────────────────────────────────────────────────

    @Test
    void get_whenL1MissAndL2Hit_warmsL1AndReturnsL2Value() {
        ValueWrapper wrapper = () -> "from-l2";
        when(l1.get("k")).thenReturn(null);
        when(l2.get("k")).thenReturn(wrapper);

        assertThat(cache.get("k")).isSameAs(wrapper);
        verify(l1).put("k", "from-l2");
    }

    @Test
    void get_whenL2HitWithNullValue_warmsL1WithNull() {
        ValueWrapper nullWrapper = () -> null;
        when(l1.get("k")).thenReturn(null);
        when(l2.get("k")).thenReturn(nullWrapper);

        cache.get("k");

        verify(l1).put("k", null);
    }

    // ── get(key) — full miss ──────────────────────────────────────────────

    @Test
    void get_whenBothMiss_returnsNull() {
        when(l1.get("k")).thenReturn(null);
        when(l2.get("k")).thenReturn(null);

        assertThat(cache.get("k")).isNull();
        verify(l1, never()).put(any(), any());
    }

    // ── get(key, Class) ───────────────────────────────────────────────────

    @Test
    void get_withType_whenL1Hit_returnsCastValue() {
        when(l1.get("k")).thenReturn(() -> "value");

        assertThat(cache.get("k", String.class)).isEqualTo("value");
    }

    @Test
    void get_withType_whenBothMiss_returnsNull() {
        when(l1.get("k")).thenReturn(null);
        when(l2.get("k")).thenReturn(null);

        assertThat(cache.get("k", String.class)).isNull();
    }

    // ── get(key, Callable) ────────────────────────────────────────────────

    @Test
    void get_withValueLoader_whenL1Hit_returnsL1ValueWithoutCallingLoader() throws Exception {
        when(l1.get("k")).thenReturn(() -> "l1-hit");
        Callable<String> loader = mock(Callable.class);

        assertThat(cache.get("k", loader)).isEqualTo("l1-hit");
        verifyNoInteractions(loader);
    }

    @Test
    void get_withValueLoader_whenL2Hit_warmsL1WithoutCallingLoader() throws Exception {
        when(l1.get("k")).thenReturn(null);
        when(l2.get("k")).thenReturn(() -> "l2-hit");
        Callable<String> loader = mock(Callable.class);

        assertThat(cache.get("k", loader)).isEqualTo("l2-hit");
        verify(l1).put("k", "l2-hit");
        verifyNoInteractions(loader);
    }

    @Test
    void get_withValueLoader_whenBothMiss_callsLoaderAndWritesToBothLevels() throws Exception {
        when(l1.get("k")).thenReturn(null);
        when(l2.get("k")).thenReturn(null);

        var result = cache.get("k", () -> "loaded");

        assertThat(result).isEqualTo("loaded");
        verify(l1).put("k", "loaded");
        verify(l2).put("k", "loaded");
    }

    @Test
    void get_withValueLoader_whenLoaderThrows_wrapsInValueRetrievalException() {
        when(l1.get("k")).thenReturn(null);
        when(l2.get("k")).thenReturn(null);

        assertThatThrownBy(() -> cache.get("k", () -> { throw new RuntimeException("db down"); }))
                .isInstanceOf(Cache.ValueRetrievalException.class)
                .cause()
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");
    }

    // ── put ───────────────────────────────────────────────────────────────

    @Test
    void put_writesBothL1AndL2() {
        cache.put("k", "v");

        verify(l1).put("k", "v");
        verify(l2).put("k", "v");
    }

    // ── evict ─────────────────────────────────────────────────────────────

    @Test
    void evict_evictsBothL1AndL2() {
        cache.evict("k");

        verify(l1).evict("k");
        verify(l2).evict("k");
    }

    // ── clear ─────────────────────────────────────────────────────────────

    @Test
    void clear_clearsBothL1AndL2() {
        cache.clear();

        verify(l1).clear();
        verify(l2).clear();
    }
}