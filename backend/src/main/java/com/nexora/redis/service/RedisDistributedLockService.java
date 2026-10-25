package com.nexora.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * PHASE 21: Redis Distributed Lock Service.
 * Provides distributed locking across multi-node/microservice deployments.
 * Prevents race conditions during high-concurrency checkout and flash sales
 * without holding prolonged database row locks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDistributedLockService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "nexora:lock:";

    /**
     * Try to acquire an exclusive distributed lock with lease time.
     */
    public boolean acquireLock(String lockKey, String lockValue, long leaseTimeSeconds) {
        String fullKey = LOCK_PREFIX + lockKey;
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(fullKey, lockValue, leaseTimeSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception ex) {
            log.warn("Redis acquireLock error for key '{}': {}", lockKey, ex.getMessage());
            return false;
        }
    }

    /**
     * Release distributed lock safely if the current lockValue matches.
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        String fullKey = LOCK_PREFIX + lockKey;
        try {
            Object currentVal = redisTemplate.opsForValue().get(fullKey);
            if (currentVal != null && currentVal.toString().equals(lockValue)) {
                return Boolean.TRUE.equals(redisTemplate.delete(fullKey));
            }
            return false;
        } catch (Exception ex) {
            log.warn("Redis releaseLock error for key '{}': {}", lockKey, ex.getMessage());
            return false;
        }
    }

    /**
     * Execute a task within a distributed lock with automatic retry and release.
     */
    public <T> T executeWithLock(String lockKey, long waitTimeMs, long leaseTimeSeconds, Supplier<T> task) {
        String lockValue = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        boolean locked = false;

        try {
            while (System.currentTimeMillis() - startTime < waitTimeMs) {
                if (acquireLock(lockKey, lockValue, leaseTimeSeconds)) {
                    locked = true;
                    break;
                }
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for distributed lock on: " + lockKey);
                }
            }

            if (!locked) {
                // If Redis is down or lock could not be acquired within timeout
                log.warn("Could not acquire distributed lock for '{}' within {}ms", lockKey, waitTimeMs);
                throw new IllegalStateException("Could not acquire distributed lock for product: " + lockKey);
            }

            return task.get();
        } finally {
            if (locked) {
                releaseLock(lockKey, lockValue);
            }
        }
    }
}
