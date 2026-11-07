package com.nexora.routing.service;

import com.nexora.routing.dto.RouteRequest;
import com.nexora.routing.dto.RouteResponse;
import com.nexora.routing.model.RoutePoint;
import com.nexora.routing.model.RoutingProfile;
import com.nexora.routing.provider.HaversineFallbackRoutingProvider;
import com.nexora.routing.provider.OsrmRoutingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Intelligent Routing Engine Service.
 *
 * Orchestrates caching, primary road-network routing (OSRM), and proximity fallback (Haversine).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final OsrmRoutingProvider osrmProvider;
    private final HaversineFallbackRoutingProvider fallbackProvider;
    private final RouteCacheService routeCacheService;

    @Value("${nexora.routing.provider:osrm}")
    private String configuredProvider = "osrm";

    /**
     * Calculates the road route between two points.
     * Checks cache -> primary provider (OSRM) -> fallback provider (Haversine).
     */
    public RouteResponse calculateRoute(RouteRequest request) {
        if (request == null || !request.isValid()) {
            throw new IllegalArgumentException("Cannot calculate route with invalid origin or destination coordinates.");
        }

        // 1. Check cache first
        Optional<RouteResponse> cached = routeCacheService.getCachedRoute(request);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. Try primary road provider (OSRM) if configured and available
        if ("osrm".equalsIgnoreCase(configuredProvider) && osrmProvider.isAvailable()) {
            try {
                RouteResponse response = osrmProvider.calculateRoute(request);
                routeCacheService.cacheRoute(request, response);
                return response;
            } catch (Exception ex) {
                log.warn("OSRM road calculation failed ({}); failing over to Haversine fallback.", ex.getMessage());
            }
        }

        // 3. Fall back gracefully to Haversine proximity calculation
        RouteResponse fallbackResponse = fallbackProvider.calculateRoute(request);
        routeCacheService.cacheRoute(request, fallbackResponse);
        return fallbackResponse;
    }

    /**
     * Convenience method with coordinate arguments.
     */
    public RouteResponse calculateRoute(
            double originLat, double originLng, double destLat, double destLng, RoutingProfile profile) {

        RouteRequest request = RouteRequest.builder()
                .origin(RoutePoint.of(originLat, originLng))
                .destination(RoutePoint.of(destLat, destLng))
                .profile(profile != null ? profile : RoutingProfile.DRIVING)
                .build();

        return calculateRoute(request);
    }

    /**
     * Returns road distance in meters between two points.
     */
    public double getRouteDistanceMeters(double originLat, double originLng, double destLat, double destLng) {
        return calculateRoute(originLat, originLng, destLat, destLng, RoutingProfile.DRIVING).getDistanceMeters();
    }

    /**
     * Returns road duration in seconds between two points.
     */
    public int getRouteDurationSeconds(double originLat, double originLng, double destLat, double destLng) {
        return calculateRoute(originLat, originLng, destLat, destLng, RoutingProfile.DRIVING).getDurationSeconds();
    }
}
