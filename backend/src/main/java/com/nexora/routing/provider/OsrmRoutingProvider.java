package com.nexora.routing.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.routing.dto.RouteRequest;
import com.nexora.routing.dto.RouteResponse;
import com.nexora.routing.model.RoutePoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Open Source Routing Machine (OSRM) HTTP Routing Provider.
 *
 * Implements road-network routing queries via standard OSRM v1 Route API.
 * Includes connection timeouts, circuit breaker protection, and coordinate validation.
 */
@Slf4j
@Component
public class OsrmRoutingProvider implements RoutingProvider {

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Circuit breaker state
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitTripTimestamp = new AtomicLong(0);
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final long CIRCUIT_RESET_MS = 60_000L; // 1 minute cooldown

    public OsrmRoutingProvider(
            @Value("${nexora.routing.osrm.base-url:https://router.project-osrm.org}") String baseUrl,
            @Value("${nexora.routing.osrm.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${nexora.routing.osrm.read-timeout-ms:2500}") int readTimeoutMs,
            ObjectMapper objectMapper) {

        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public RouteResponse calculateRoute(RouteRequest request) {
        if (!isAvailable()) {
            throw new IllegalStateException("OSRM provider is currently tripped by circuit breaker.");
        }

        RoutePoint origin = request.getOrigin();
        RoutePoint dest = request.getDestination();

        if (!request.isValid()) {
            throw new IllegalArgumentException("Invalid origin or destination coordinates for OSRM route.");
        }

        String profile = request.getProfile() != null ? request.getProfile().getOsrmProfile() : "driving";

        // OSRM expects coordinates in {longitude},{latitude} format
        String uri = String.format(Locale.US, "%s/route/v1/%s/%.6f,%.6f;%.6f,%.6f?overview=simplified&geometries=polyline",
                baseUrl,
                profile,
                origin.getLongitude(), origin.getLatitude(),
                dest.getLongitude(), dest.getLatitude());

        try {
            long startTime = System.currentTimeMillis();
            String rawJson = restTemplate.getForObject(uri, String.class);
            long elapsed = System.currentTimeMillis() - startTime;

            JsonNode root = objectMapper.readTree(rawJson);
            String code = root.path("code").asText("");

            if (!"Ok".equalsIgnoreCase(code)) {
                throw new RuntimeException("OSRM returned non-OK status: " + code);
            }

            JsonNode firstRoute = root.path("routes").get(0);
            if (firstRoute == null || firstRoute.isMissingNode()) {
                throw new RuntimeException("OSRM returned empty routes array.");
            }

            double distanceMeters = firstRoute.path("distance").asDouble(0.0);
            double durationSeconds = firstRoute.path("duration").asDouble(0.0);
            String geometry = firstRoute.path("geometry").asText(null);

            // Reset circuit breaker on success
            consecutiveFailures.set(0);
            log.debug("OSRM Route calculated in {}ms: {}m, {}s", elapsed, distanceMeters, durationSeconds);

            return RouteResponse.builder()
                    .distanceMeters(distanceMeters)
                    .durationSeconds((int) Math.round(durationSeconds))
                    .geometry(geometry)
                    .routingProvider(getProviderName())
                    .isFallbackEstimate(false)
                    .routeStatus("OK")
                    .build();

        } catch (Exception ex) {
            int failures = consecutiveFailures.incrementAndGet();
            if (failures >= MAX_CONSECUTIVE_FAILURES) {
                circuitTripTimestamp.set(System.currentTimeMillis());
                log.warn("OSRM Circuit breaker TRIPPED after {} consecutive failures. Error: {}", failures, ex.getMessage());
            } else {
                log.warn("OSRM routing request failed (attempt {}): {}", failures, ex.getMessage());
            }
            throw new RuntimeException("OSRM routing execution failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getProviderName() {
        return "OSRM";
    }

    @Override
    public boolean isAvailable() {
        if (consecutiveFailures.get() < MAX_CONSECUTIVE_FAILURES) {
            return true;
        }
        long trippedAt = circuitTripTimestamp.get();
        if (System.currentTimeMillis() - trippedAt > CIRCUIT_RESET_MS) {
            // Half-open attempt: allow one retry
            consecutiveFailures.set(MAX_CONSECUTIVE_FAILURES - 1);
            return true;
        }
        return false;
    }
}
