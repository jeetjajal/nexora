package com.nexora.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long timeoutSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
            log.debug("Cached key '{}' with TTL {}s", key, timeoutSeconds);
        } catch (Exception ex) {
            log.warn("Redis write failed for key '{}': {}. Continuing without cache.", key, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && clazz.isInstance(value)) {
                log.debug("Cache HIT for key '{}'", key);
                return Optional.of((T) value);
            }
            log.debug("Cache MISS for key '{}'", key);
        } catch (Exception ex) {
            log.warn("Redis read failed for key '{}': {}. Falling back to database.", key, ex.getMessage());
        }
        return Optional.empty();
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Evicted cache key '{}'", key);
        } catch (Exception ex) {
            log.warn("Redis delete failed for key '{}': {}", key, ex.getMessage());
        }
    }

    public boolean hasKey(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception ex) {
            log.warn("Redis hasKey failed for key '{}': {}", key, ex.getMessage());
            return false;
        }
    }

    /**
     * Cache-Aside helper: tries to fetch from Redis; on cache miss, executes the database supplier,
     * caches the result, and returns it.
     */
    public <T> T getWithFallback(String key, Class<T> clazz, long ttlSeconds, Supplier<T> dbFallback) {
        Optional<T> cached = get(key, clazz);
        if (cached.isPresent()) {
            return cached.get();
        }

        T result = dbFallback.get();
        if (result != null) {
            set(key, result, ttlSeconds);
        }
        return result;
    }
}
