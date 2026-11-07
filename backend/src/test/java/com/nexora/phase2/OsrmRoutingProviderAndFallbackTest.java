package com.nexora.phase2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.redis.service.RedisCacheService;
import com.nexora.routing.dto.RouteRequest;
import com.nexora.routing.dto.RouteResponse;
import com.nexora.routing.model.RoutePoint;
import com.nexora.routing.model.RoutingProfile;
import com.nexora.routing.provider.HaversineFallbackRoutingProvider;
import com.nexora.routing.provider.OsrmRoutingProvider;
import com.nexora.routing.service.RouteCacheService;
import com.nexora.routing.service.RoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 2: WS1 — OSRM Routing, Fallback & Cache Tests")
class OsrmRoutingProviderAndFallbackTest {

    @Mock private RestTemplate restTemplate;
    @Mock private RedisCacheService redisCacheService;

    private ObjectMapper objectMapper;
    private OsrmRoutingProvider osrmProvider;
    private HaversineFallbackRoutingProvider fallbackProvider;
    private RouteCacheService routeCacheService;

    // Test coordinates: Store (Connaught Place, Delhi) to Customer (Barakhamba Road, Delhi)
    private final RoutePoint storePoint = RoutePoint.of(28.6315, 77.2167);
    private final RoutePoint customerPoint = RoutePoint.of(28.6289, 77.2285);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        osrmProvider = new OsrmRoutingProvider("https://router.project-osrm.org", 2000, 2500, objectMapper);
        ReflectionTestUtils.setField(osrmProvider, "restTemplate", restTemplate);

        fallbackProvider = new HaversineFallbackRoutingProvider(25.0);
        routeCacheService = new RouteCacheService(redisCacheService);
    }

    @Test
    @DisplayName("OSRM Provider parses valid JSON response into road distance and duration")
    void testOsrmProviderParsesValidResponse() {
        String mockOsrmJson = """
            {
              "code": "Ok",
              "routes": [
                {
                  "geometry": "g_m`F|`_tMvAkCh@iA",
                  "legs": [],
                  "weight_name": "routability",
                  "weight": 340.2,
                  "duration": 312.0,
                  "distance": 1420.8
                }
              ],
              "waypoints": []
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockOsrmJson);

        RouteRequest request = RouteRequest.builder()
                .origin(storePoint)
                .destination(customerPoint)
                .profile(RoutingProfile.DRIVING)
                .build();

        RouteResponse response = osrmProvider.calculateRoute(request);

        assertNotNull(response);
        assertEquals("OSRM", response.getRoutingProvider());
        assertFalse(response.isFallbackEstimate());
        assertEquals(1420.8, response.getDistanceMeters(), 0.1);
        assertEquals(312, response.getDurationSeconds());
        assertEquals("g_m`F|`_tMvAkCh@iA", response.getGeometry());
    }

    @Test
    @DisplayName("OSRM Provider trips circuit breaker on repeated network timeouts")
    void testOsrmCircuitBreakerTripsOnTimeouts() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        RouteRequest request = RouteRequest.builder()
                .origin(storePoint)
                .destination(customerPoint)
                .profile(RoutingProfile.DRIVING)
                .build();

        // 5 consecutive failures trip circuit breaker
        for (int i = 0; i < 5; i++) {
            assertThrows(RuntimeException.class, () -> osrmProvider.calculateRoute(request));
        }

        // 6th call is blocked by circuit breaker
        assertFalse(osrmProvider.isAvailable());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> osrmProvider.calculateRoute(request));
        assertTrue(ex.getMessage().contains("tripped by circuit breaker"));
    }

    @Test
    @DisplayName("Invalid or (0,0) coordinates are rejected with IllegalArgumentException")
    void testInvalidCoordinatesRejected() {
        RouteRequest request = RouteRequest.builder()
                .origin(new RoutePoint(0.0, 0.0))
                .destination(customerPoint)
                .build();

        assertThrows(IllegalArgumentException.class, () -> osrmProvider.calculateRoute(request));
    }

    @Test
    @DisplayName("Haversine fallback calculates approximate distance and sets isFallbackEstimate=true")
    void testHaversineFallbackCalculatesDistanceAndSetsFlag() {
        RouteRequest request = RouteRequest.builder()
                .origin(storePoint)
                .destination(customerPoint)
                .profile(RoutingProfile.BIKE)
                .build();

        RouteResponse response = fallbackProvider.calculateRoute(request);

        assertNotNull(response);
        assertEquals("HAVERSINE_FALLBACK", response.getRoutingProvider());
        assertTrue(response.isFallbackEstimate());
        assertTrue(response.getDistanceMeters() > 500 && response.getDistanceMeters() < 3000,
                "Haversine road-approximated distance should be in 0.5km - 3km range");
        assertTrue(response.getDurationSeconds() > 60, "Duration should be positive realistic time");
    }

    @Test
    @DisplayName("RouteCacheService caches route and avoids duplicate external lookups")
    void testRouteCacheServiceHit() {
        RouteRequest request = RouteRequest.builder()
                .origin(storePoint)
                .destination(customerPoint)
                .profile(RoutingProfile.DRIVING)
                .build();

        RouteResponse cachedResponse = RouteResponse.builder()
                .distanceMeters(1420.0)
                .durationSeconds(310)
                .routingProvider("OSRM_CACHED")
                .isFallbackEstimate(false)
                .routeStatus("Ok")
                .build();

        when(redisCacheService.get(anyString(), eq(RouteResponse.class)))
                .thenReturn(Optional.of(cachedResponse));

        Optional<RouteResponse> retrieved = routeCacheService.getCachedRoute(request);
        assertTrue(retrieved.isPresent());
        assertEquals(1420.0, retrieved.get().getDistanceMeters());
        assertEquals("OSRM_CACHED", retrieved.get().getRoutingProvider());
    }

    @Test
    @DisplayName("RoutingService falls back to Haversine when OSRM fails")
    void testRoutingServiceGracefulFallback() {
        RoutingService routingService = new RoutingService(
                osrmProvider,
                fallbackProvider,
                routeCacheService
        );

        when(redisCacheService.get(anyString(), eq(RouteResponse.class))).thenReturn(Optional.empty());

        // OSRM throws exception
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new ResourceAccessException("OSRM unreachable"));

        RouteResponse response = routingService.calculateRoute(
                storePoint.getLatitude(), storePoint.getLongitude(),
                customerPoint.getLatitude(), customerPoint.getLongitude(),
                RoutingProfile.DRIVING
        );

        assertNotNull(response);
        assertEquals("HAVERSINE_FALLBACK", response.getRoutingProvider());
        assertTrue(response.isFallbackEstimate());
        assertTrue(response.getDistanceMeters() > 0);
    }
}
