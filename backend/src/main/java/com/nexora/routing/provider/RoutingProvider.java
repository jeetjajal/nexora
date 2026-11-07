package com.nexora.routing.provider;

import com.nexora.routing.dto.RouteRequest;
import com.nexora.routing.dto.RouteResponse;

/**
 * Standard abstraction for road-network routing providers.
 */
public interface RoutingProvider {

    /**
     * Calculates the road-network route between origin and destination.
     */
    RouteResponse calculateRoute(RouteRequest request);

    /**
     * Unique identifier for the provider (e.g. OSRM, GOOGLE_MAPS, HAVERSINE_FALLBACK).
     */
    String getProviderName();

    /**
     * Whether the provider is currently operational and available.
     */
    boolean isAvailable();
}
