package com.nexora.redis.service;

import com.nexora.cart.dto.CartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCartCacheService {

    private final RedisCacheService cacheService;

    @Value("${nexora.cache.ttl.cart:1800}")
    private long cartTtlSeconds;

    private static final String CART_KEY_PREFIX = "nexora:cart:user:";

    public Optional<CartResponse> getCachedCart(Long userId) {
        String key = CART_KEY_PREFIX + userId;
        return cacheService.get(key, CartResponse.class);
    }

    public void cacheCart(Long userId, CartResponse cart) {
        if (userId == null || cart == null) {
            return;
        }
        String key = CART_KEY_PREFIX + userId;
        cacheService.set(key, cart, cartTtlSeconds);
        log.debug("Saved cart to Redis cache for userId: {}", userId);
    }

    public void evictCart(Long userId) {
        if (userId == null) {
            return;
        }
        String key = CART_KEY_PREFIX + userId;
        cacheService.delete(key);
        log.debug("Evicted cart from Redis cache for userId: {}", userId);
    }
}
