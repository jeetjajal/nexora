package com.nexora.phase15;

import com.nexora.redis.service.RedisCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 15 - RedisCacheService Unit Tests")
class RedisCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisCacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new RedisCacheService(redisTemplate);
    }

    @Test
    @DisplayName("Should set key with specified TTL in Redis")
    void testSet() {
        cacheService.set("test:key", "testValue", 300);
        verify(valueOperations).set("test:key", "testValue", 300, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should return cached value when cache HIT")
    void testGetHit() {
        when(valueOperations.get("test:key")).thenReturn("cachedValue");

        Optional<String> result = cacheService.get("test:key", String.class);

        assertThat(result).isPresent().contains("cachedValue");
    }

    @Test
    @DisplayName("Should return empty when cache MISS")
    void testGetMiss() {
        when(valueOperations.get("missing:key")).thenReturn(null);

        Optional<String> result = cacheService.get("missing:key", String.class);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should delete key from Redis")
    void testDelete() {
        cacheService.delete("test:key");
        verify(redisTemplate).delete("test:key");
    }

    @Test
    @DisplayName("Cache-Aside: should use database fallback and populate cache on cache miss")
    void testGetWithFallbackOnMiss() {
        when(valueOperations.get("store:1")).thenReturn(null);

        AtomicInteger dbHits = new AtomicInteger(0);
        String result = cacheService.getWithFallback("store:1", String.class, 600, () -> {
            dbHits.incrementAndGet();
            return "DbStoreData";
        });

        assertThat(result).isEqualTo("DbStoreData");
        assertThat(dbHits.get()).isEqualTo(1);
        verify(valueOperations).set("store:1", "DbStoreData", 600, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Cache-Aside: should return cached value without hitting database on cache hit")
    void testGetWithFallbackOnHit() {
        when(valueOperations.get("store:1")).thenReturn("CachedStoreData");

        AtomicInteger dbHits = new AtomicInteger(0);
        String result = cacheService.getWithFallback("store:1", String.class, 600, () -> {
            dbHits.incrementAndGet();
            return "DbStoreData";
        });

        assertThat(result).isEqualTo("CachedStoreData");
        assertThat(dbHits.get()).isEqualTo(0);
        verify(valueOperations, never()).set(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("Graceful Resilience: should fall back to DB supplier if Redis throws an exception")
    void testFallbackOnRedisException() {
        when(valueOperations.get(any())).thenThrow(new RuntimeException("Connection refused"));

        String result = cacheService.getWithFallback("store:1", String.class, 600, () -> "ResilientData");

        assertThat(result).isEqualTo("ResilientData");
    }
}
