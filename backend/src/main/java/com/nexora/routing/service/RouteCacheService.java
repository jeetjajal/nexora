package com.nexora.routing.service;

import com.nexora.redis.service.RedisCacheService;
import com.nexora.routing.dto.RouteRequest;
import com.nexora.routing.dto.RouteResponse;
import com.nexora.routing.model.RoutePoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Route Caching Service.
 *
 * Caches frequently calculated delivery routes (e.g. from dark store hubs to residential clusters)
 * by rounding coordinate keys to 4 decimal places (~11 meters).
 * Backed by Redis when available, with a resilient bounded in-memory LRU fallback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCacheService {

    private final RedisCacheService redisCacheService;

    @Value("${nexora.routing.cache.ttl-seconds:1800}")
    private long cacheTtlSeconds = 1800;

    // Resilient local memory cache fallback if Redis is unreachable
    private static final int MAX_LOCAL_CACHE_ENTRIES = 5000;
    private final Map<String, LocalCacheEntry> localMemoryCache = new ConcurrentHashMap<>();

    private record LocalCacheEntry(RouteResponse response, long expiresAtMillis) {}

    public Optional<RouteResponse> getCachedRoute(RouteRequest request) {
        String cacheKey = generateCacheKey(request);

        // 1. Try Redis first
        try {
            Optional<RouteResponse> redisCached = redisCacheService.get(cacheKey, RouteResponse.class);
            if (redisCached.isPresent()) {
                log.debug("Route cache HIT (Redis): {}", cacheKey);
                return redisCached;
            }
        } catch (Exception ex) {
            log.debug("Redis route cache read bypassed: {}", ex.getMessage());
        }

        // 2. Fall back to local memory cache
        LocalCacheEntry localEntry = localMemoryCache.get(cacheKey);
        if (localEntry != null) {
            if (System.currentTimeMillis() < localEntry.expiresAtMillis()) {
                log.debug("Route cache HIT (Memory): {}", cacheKey);
                return Optional.of(localEntry.response());
            } else {
                localMemoryCache.remove(cacheKey);
            }
        }

        log.debug("Route cache MISS: {}", cacheKey);
        return Optional.empty();
    }

    public void cacheRoute(RouteRequest request, RouteResponse response) {
        if (response == null || "FAILED".equalsIgnoreCase(response.getRouteStatus())) {
            return; // Do not cache failed routes
        }

        String cacheKey = generateCacheKey(request);

        // 1. Store in Redis
        try {
            redisCacheService.set(cacheKey, response, cacheTtlSeconds);
        } catch (Exception ex) {
            log.debug("Redis route cache write bypassed: {}", ex.getMessage());
        }

        // 2. Also keep in bounded local memory cache
        if (localMemoryCache.size() >= MAX_LOCAL_CACHE_ENTRIES) {
            // Evict expired entries
            long now = System.currentTimeMillis();
            localMemoryCache.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() < now);
            if (localMemoryCache.size() >= MAX_LOCAL_CACHE_ENTRIES) {
                localMemoryCache.clear(); // Safety valve against unbounded growth
            }
        }

        long expiresAtMillis = System.currentTimeMillis() + (cacheTtlSeconds * 1000L);
        localMemoryCache.put(cacheKey, new LocalCacheEntry(response, expiresAtMillis));
    }

    public void evictRoute(RouteRequest request) {
        String cacheKey = generateCacheKey(request);
        try {
            redisCacheService.delete(cacheKey);
        } catch (Exception ignored) {}
        localMemoryCache.remove(cacheKey);
    }

    public void clearLocalMemoryCache() {
        localMemoryCache.clear();
    }

    private String generateCacheKey(RouteRequest request) {
        RoutePoint o = request.getOrigin();
        RoutePoint d = request.getDestination();
        String profile = request.getProfile() != null ? request.getProfile().name().toLowerCase() : "driving";

        // Coordinates rounded to 4 decimals (~11m resolution)
        return String.format(Locale.US, "nexora:route:%s:%.4f,%.4f:%.4f,%.4f",
                profile,
                o.getLatitude(), o.getLongitude(),
                d.getLatitude(), d.getLongitude());
    }
}
