package com.nexora.phase2;

import com.nexora.delivery.dto.NearbyRiderDto;
import com.nexora.delivery.dto.RiderTelemetryDto;
import com.nexora.delivery.entity.DeliveryLocation;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.DeliveryPartnerStatus;
import com.nexora.delivery.repository.DeliveryLocationRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.delivery.service.RiderLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 2: WS2 — Redis Geospatial Rider Tracking & Telemetry Tests")
class RedisGeospatialRiderTrackingTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private DeliveryPartnerRepository partnerRepository;
    @Mock private DeliveryLocationRepository locationRepository;
    @Mock private GeoOperations<String, Object> geoOperations;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ZSetOperations<String, Object> zSetOperations;

    private RiderLocationService riderLocationService;

    private final Long riderId = 101L;
    private final double riderLat = 28.6315;
    private final double riderLng = 77.2167;

    @BeforeEach
    void setUp() {
        riderLocationService = new RiderLocationService(redisTemplate, partnerRepository, locationRepository);
    }

    @Test
    @DisplayName("updateRiderLocation adds coordinate to Redis GEO and writes hash metadata")
    void testUpdateRiderLocationSuccess() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        DeliveryPartner mockPartner = new DeliveryPartner();
        mockPartner.setId(riderId);
        when(partnerRepository.findById(riderId)).thenReturn(Optional.of(mockPartner));

        RiderTelemetryDto result = riderLocationService.updateRiderLocation(
                riderId, riderLat, riderLng, 24.5, 180.0);

        assertNotNull(result);
        assertEquals(riderId, result.getRiderId());
        assertEquals(riderLat, result.getLatitude());
        assertEquals(riderLng, result.getLongitude());
        assertTrue(result.isFresh());

        // Verify Redis GEO add
        verify(geoOperations).add(eq("nexora:riders:geo"), any(Point.class), eq(riderId.toString()));
        // Verify Redis Hash write
        verify(hashOperations).putAll(eq("nexora:rider:meta:101"), anyMap());
        // Verify 300s TTL set
        verify(redisTemplate).expire("nexora:rider:meta:101", 300, TimeUnit.SECONDS);
        // Verify DB update
        verify(partnerRepository).save(any(DeliveryPartner.class));
        verify(locationRepository).save(any(DeliveryLocation.class));
    }

    @Test
    @DisplayName("Database write is throttled to at most once per 60 seconds")
    void testDatabaseThrottling() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        DeliveryPartner mockPartner = new DeliveryPartner();
        mockPartner.setId(riderId);
        when(partnerRepository.findById(riderId)).thenReturn(Optional.of(mockPartner));

        riderLocationService.updateRiderLocation(riderId, riderLat, riderLng, 20.0, 90.0);
        riderLocationService.updateRiderLocation(riderId, riderLat + 0.0001, riderLng + 0.0001, 22.0, 95.0);

        // DB save should be called only ONCE because calls occur immediately within 60s throttle limit
        verify(partnerRepository, times(1)).save(any(DeliveryPartner.class));
    }

    @Test
    @DisplayName("getRiderLocation returns fresh status for recent telemetry and stale for >90s")
    void testFreshnessCheck() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // 1. Fresh telemetry (30 seconds ago)
        Map<Object, Object> freshMeta = new HashMap<>();
        freshMeta.put("latitude", String.valueOf(riderLat));
        freshMeta.put("longitude", String.valueOf(riderLng));
        freshMeta.put("updatedAt", String.valueOf(System.currentTimeMillis() - 30_000));
        when(hashOperations.entries("nexora:rider:meta:101")).thenReturn(freshMeta);

        Optional<RiderTelemetryDto> freshDto = riderLocationService.getRiderLocation(riderId);
        assertTrue(freshDto.isPresent());
        assertTrue(freshDto.get().isFresh());

        // 2. Stale telemetry (120 seconds ago)
        Map<Object, Object> staleMeta = new HashMap<>();
        staleMeta.put("latitude", String.valueOf(riderLat));
        staleMeta.put("longitude", String.valueOf(riderLng));
        staleMeta.put("updatedAt", String.valueOf(System.currentTimeMillis() - 120_000));
        when(hashOperations.entries("nexora:rider:meta:101")).thenReturn(staleMeta);

        Optional<RiderTelemetryDto> staleDto = riderLocationService.getRiderLocation(riderId);
        assertTrue(staleDto.isPresent());
        assertFalse(staleDto.get().isFresh());
    }

    @Test
    @DisplayName("findNearbyAvailableRiders queries Redis GEO and returns candidate riders")
    void testFindNearbyAvailableRiders() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult =
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("101", new Point(riderLng, riderLat)), new Distance(1.2, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults =
                new GeoResults<>(List.of(geoResult));

        when(geoOperations.radius(eq("nexora:riders:geo"), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(geoResults);

        Map<Object, Object> meta = new HashMap<>();
        meta.put("latitude", String.valueOf(riderLat));
        meta.put("longitude", String.valueOf(riderLng));
        meta.put("status", "AVAILABLE");
        meta.put("isOnline", "true");
        meta.put("updatedAt", String.valueOf(System.currentTimeMillis()));

        when(hashOperations.entries("nexora:rider:meta:101")).thenReturn(meta);

        List<NearbyRiderDto> candidates = riderLocationService.findNearbyAvailableRiders(riderLat, riderLng, 3.0);

        assertNotNull(candidates);
        assertEquals(1, candidates.size());
        assertEquals(101L, candidates.get(0).getRiderId());
        assertEquals(1.2, candidates.get(0).getDistanceKm(), 0.1);
        assertTrue(candidates.get(0).isFresh());
    }

    @Test
    @DisplayName("removeRiderLocation evicts rider from GEO and deletes metadata on offline")
    void testRemoveRiderLocation() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);

        riderLocationService.removeRiderLocation(riderId);

        verify(geoOperations).remove("nexora:riders:geo", "101");
        verify(redisTemplate).delete("nexora:rider:meta:101");
    }

    @Test
    @DisplayName("Redis failure degrades gracefully to MySQL without crashing update")
    void testGracefulDegradationOnRedisFailure() {
        when(redisTemplate.opsForGeo()).thenThrow(new RuntimeException("Redis connection refused"));

        DeliveryPartner mockPartner = new DeliveryPartner();
        mockPartner.setId(riderId);
        when(partnerRepository.findById(riderId)).thenReturn(Optional.of(mockPartner));

        // Must not throw exception
        assertDoesNotThrow(() -> riderLocationService.updateRiderLocation(
                riderId, riderLat, riderLng, 15.0, 0.0));

        // Fallback DB save must still execute
        verify(partnerRepository).save(any(DeliveryPartner.class));
    }
}
