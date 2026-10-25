package com.nexora.phase15;

import com.nexora.redis.service.RedisRateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 15 - RedisRateLimiterService Unit Tests")
class RedisRateLimiterServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisRateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimiterService = new RedisRateLimiterService(redisTemplate);
    }

    @Test
    @DisplayName("First request sets TTL on key and returns allowed")
    void testFirstRequestSetsExpiry() {
        when(valueOperations.increment("nexora:ratelimit:ip_192.168.1.1")).thenReturn(1L);

        boolean allowed = rateLimiterService.isAllowed("ip_192.168.1.1", 10, 60);

        assertThat(allowed).isTrue();
        verify(redisTemplate).expire("nexora:ratelimit:ip_192.168.1.1", 60, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Request count within limit returns allowed without resetting expiry")
    void testRequestWithinLimit() {
        when(valueOperations.increment("nexora:ratelimit:user_123")).thenReturn(5L);

        boolean allowed = rateLimiterService.isAllowed("user_123", 10, 60);

        assertThat(allowed).isTrue();
        verify(redisTemplate, never()).expire(any(), anyLong(), any());
    }

    @Test
    @DisplayName("Request count exceeding limit returns false (blocked)")
    void testRequestExceedingLimit() {
        when(valueOperations.increment("nexora:ratelimit:user_123")).thenReturn(11L);

        boolean allowed = rateLimiterService.isAllowed("user_123", 10, 60);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Redis exception fails open gracefully (allows request)")
    void testFailOpenOnRedisException() {
        when(valueOperations.increment(any())).thenThrow(new RuntimeException("Redis unavailable"));

        boolean allowed = rateLimiterService.isAllowed("user_123", 10, 60);

        assertThat(allowed).isTrue();
    }
}
