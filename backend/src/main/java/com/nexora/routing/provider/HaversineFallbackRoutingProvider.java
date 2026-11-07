package com.nexora.routing.provider;

import com.nexora.routing.dto.RouteRequest;
import com.nexora.routing.dto.RouteResponse;
import com.nexora.routing.model.RoutePoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Haversine Great-Circle Proximity Fallback Provider.
 *
 * Used exclusively when road-network routing (OSRM) is unreachable, unconfigured, or timed out.
 * Explicitly marks responses with isFallbackEstimate = true so callers and customers know
 * that estimated travel duration is an approximation rather than true road navigation.
 */
@Slf4j
@Component
public class HaversineFallbackRoutingProvider implements RoutingProvider {

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private final double averageSpeedKmh;

    public HaversineFallbackRoutingProvider(
            @Value("${nexora.routing.fallback.average-speed-kmh:20.0}") double averageSpeedKmh) {
        this.averageSpeedKmh = averageSpeedKmh > 0 ? averageSpeedKmh : 20.0;
    }

    @Override
    public RouteResponse calculateRoute(RouteRequest request) {
        RoutePoint origin = request.getOrigin();
        RoutePoint dest = request.getDestination();

        if (origin == null || dest == null) {
            throw new IllegalArgumentException("Origin and destination must not be null for fallback route calculation.");
        }

        double distanceMeters = calculateHaversineDistance(
                origin.getLatitude(), origin.getLongitude(),
                dest.getLatitude(), dest.getLongitude()
        );

        // Speed in m/s = (km/h) * (1000 / 3600)
        double speedMps = (averageSpeedKmh * 1000.0) / 3600.0;
        int durationSeconds = (int) Math.round(distanceMeters / speedMps);

        log.debug("Calculated fallback Haversine route: {}m (~{}s at {} km/h)",
                Math.round(distanceMeters), durationSeconds, averageSpeedKmh);

        return RouteResponse.builder()
                .distanceMeters(distanceMeters)
                .durationSeconds(durationSeconds)
                .geometry(null) // Straight-line approximation has no road polyline
                .routingProvider(getProviderName())
                .isFallbackEstimate(true)
                .routeStatus("FALLBACK_ESTIMATE")
                .build();
    }

    @Override
    public String getProviderName() {
        return "HAVERSINE_FALLBACK";
    }

    @Override
    public boolean isAvailable() {
        return true; // Always operational locally
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
