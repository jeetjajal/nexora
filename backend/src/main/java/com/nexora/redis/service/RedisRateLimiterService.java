package com.nexora.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "nexora:ratelimit:";

    /**
     * Checks if a request is allowed under the rate limit window.
     * Uses atomic INCR and sets TTL on the initial key creation.
     *
     * @param clientIdentifier e.g. IP address, userId, or API key
     * @param maxRequests maximum allowed requests within the window
     * @param windowSeconds time window in seconds
     * @return true if request is within limits, false if rate limit exceeded
     */
    public boolean isAllowed(String clientIdentifier, int maxRequests, int windowSeconds) {
        String key = RATE_LIMIT_PREFIX + clientIdentifier;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                // First request in this window: set expiration
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            if (count != null && count > maxRequests) {
                log.warn("Rate limit exceeded for key '{}': {}/{} requests in {}s",
                        clientIdentifier, count, maxRequests, windowSeconds);
                return false;
            }
            return true;
        } catch (Exception ex) {
            // Fail-open: if Redis encounters an error, allow the request so users aren't locked out
            log.warn("Rate limiter failed for key '{}': {}. Defaulting to ALLOW.", clientIdentifier, ex.getMessage());
            return true;
        }
    }

    public long getRemainingRequests(String clientIdentifier, int maxRequests) {
        String key = RATE_LIMIT_PREFIX + clientIdentifier;
        try {
            Object val = redisTemplate.opsForValue().get(key);
            if (val instanceof Number) {
                long current = ((Number) val).longValue();
                return Math.max(0, maxRequests - current);
            }
        } catch (Exception ignored) {
        }
        return maxRequests;
    }
}
