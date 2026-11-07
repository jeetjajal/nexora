package com.nexora.delivery.service;

import com.nexora.delivery.dto.NearbyRiderDto;
import com.nexora.delivery.dto.RiderTelemetryDto;
import com.nexora.delivery.entity.DeliveryLocation;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.DeliveryPartnerStatus;
import com.nexora.delivery.repository.DeliveryLocationRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * REDIS GEOSPATIAL RIDER TRACKING SERVICE (NEXORA 3.0)
 *
 * Intercepts high-frequency rider GPS telemetry (every 5-10s) and writes to Redis
 * Geospatial index (GEOADD) and ephemeral telemetry hashes, eliminating MySQL connection pool
 * starvation while enabling sub-millisecond proximity queries.
 *
 * Implements:
 * - High-speed spatial indexing via Redis GEO.
 * - Stale location detection (>90s).
 * - Bounded durability throttling to MySQL (max 1 DB write/minute per rider).
 * - Graceful degradation when Redis is offline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiderLocationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeliveryPartnerRepository partnerRepository;
    private final DeliveryLocationRepository locationRepository;

    private static final String REDIS_GEO_KEY = "nexora:riders:geo";
    private static final String REDIS_META_PREFIX = "nexora:rider:meta:";

    @Value("${nexora.rider.telemetry.ttl-seconds:300}")
    private long telemetryTtlSeconds = 300L; // 5 minutes

    @Value("${nexora.rider.telemetry.stale-threshold-seconds:90}")
    private long staleThresholdSeconds = 90L; // 90 seconds

    @Value("${nexora.rider.telemetry.db-throttle-seconds:60}")
    private long dbThrottleSeconds = 60L; // Write to MySQL at most once per 60s

    // Per-rider timestamp of last durable MySQL write to prevent DB starvation
    private final Map<Long, Long> lastDurableWriteTimestamps = new ConcurrentHashMap<>();

    /**
     * Ingests a live GPS location ping from an authenticated rider.
     */
    public RiderTelemetryDto updateRiderLocation(
            Long riderId, double latitude, double longitude, Double speedKmh, Double heading) {

        if (!RiderTelemetryDto.isValidCoordinate(latitude, longitude)) {
            throw new IllegalArgumentException("Invalid GPS coordinates: (" + latitude + ", " + longitude + ")");
        }

        long now = System.currentTimeMillis();

        // 1. Write to Redis Geospatial Index & Metadata Hash
        try {
            // Redis GEO: Point(longitude, latitude)
            redisTemplate.opsForGeo().add(REDIS_GEO_KEY, new Point(longitude, latitude), String.valueOf(riderId));

            String metaKey = REDIS_META_PREFIX + riderId;
            Map<String, String> meta = new HashMap<>();
            meta.put("riderId", String.valueOf(riderId));
            meta.put("latitude", String.valueOf(latitude));
            meta.put("longitude", String.valueOf(longitude));
            meta.put("speedKmh", speedKmh != null ? String.valueOf(speedKmh) : "0.0");
            meta.put("heading", heading != null ? String.valueOf(heading) : "0.0");
            meta.put("updatedAt", String.valueOf(now));
            meta.put("status", DeliveryPartnerStatus.AVAILABLE.name());
            meta.put("isOnline", "true");

            redisTemplate.opsForHash().putAll(metaKey, meta);
            redisTemplate.expire(metaKey, telemetryTtlSeconds, TimeUnit.SECONDS);

            log.debug("Updated Redis GEO for rider #{}: ({}, {})", riderId, latitude, longitude);
        } catch (Exception ex) {
            log.warn("Redis GEO update failed for rider #{}: {}. Falling back to database.", riderId, ex.getMessage());
        }

        // 2. Throttled Durable Write to MySQL (Every 60s max or first ping)
        Long lastWrite = lastDurableWriteTimestamps.get(riderId);
        if (lastWrite == null || (now - lastWrite) > (dbThrottleSeconds * 1000L)) {
            persistDurableLocation(riderId, latitude, longitude, now);
        }

        return RiderTelemetryDto.builder()
                .riderId(riderId)
                .latitude(latitude)
                .longitude(longitude)
                .speedKmh(speedKmh)
                .heading(heading)
                .updatedAtEpochMillis(now)
                .isFresh(true)
                .ageSeconds(0L)
                .build();
    }

    /**
     * Retrieves the latest telemetry for a rider.
     */
    public Optional<RiderTelemetryDto> getRiderLocation(Long riderId) {
        String metaKey = REDIS_META_PREFIX + riderId;

        // 1. Check Redis metadata
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(metaKey);
            if (entries != null && !entries.isEmpty() && entries.containsKey("latitude")) {
                double lat = Double.parseDouble(entries.get("latitude").toString());
                double lng = Double.parseDouble(entries.get("longitude").toString());
                long updatedAt = Long.parseLong(entries.getOrDefault("updatedAt", "0").toString());
                Double speed = entries.get("speedKmh") != null ? Double.parseDouble(entries.get("speedKmh").toString()) : null;
                Double heading = entries.get("heading") != null ? Double.parseDouble(entries.get("heading").toString()) : null;

                long ageSeconds = Math.max(0, (System.currentTimeMillis() - updatedAt) / 1000L);
                boolean isFresh = ageSeconds <= staleThresholdSeconds;

                return Optional.of(RiderTelemetryDto.builder()
                        .riderId(riderId)
                        .latitude(lat)
                        .longitude(lng)
                        .speedKmh(speed)
                        .heading(heading)
                        .updatedAtEpochMillis(updatedAt)
                        .isFresh(isFresh)
                        .ageSeconds(ageSeconds)
                        .build());
            }
        } catch (Exception ex) {
            log.warn("Redis telemetry read failed for rider #{}: {}", riderId, ex.getMessage());
        }

        // 2. Fall back to MySQL DeliveryPartner entity
        return partnerRepository.findById(riderId)
                .filter(p -> p.getCurrentLat() != null && p.getCurrentLng() != null)
                .filter(p -> RiderTelemetryDto.isValidCoordinate(p.getCurrentLat(), p.getCurrentLng()))
                .map(p -> RiderTelemetryDto.builder()
                        .riderId(p.getId())
                        .latitude(p.getCurrentLat())
                        .longitude(p.getCurrentLng())
                        .isFresh(false) // Database-backed coordinates without fresh timestamp marked as not fresh
                        .ageSeconds(120L)
                        .build());
    }

    /**
     * Finds nearby available riders using Redis Geospatial radius query.
     */
    public List<NearbyRiderDto> findNearbyAvailableRiders(double centerLat, double centerLng, double radiusKm) {
        List<NearbyRiderDto> results = new ArrayList<>();
        long now = System.currentTimeMillis();

        // 1. Query Redis GEO index
        try {
            Circle searchCircle = new Circle(new Point(centerLng, centerLat), new Distance(radiusKm, Metrics.KILOMETERS));
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .includeDistance()
                    .includeCoordinates()
                    .sortAscending();

            GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults =
                    redisTemplate.opsForGeo().radius(REDIS_GEO_KEY, searchCircle, args);

            if (geoResults != null && !geoResults.getContent().isEmpty()) {
                for (GeoResult<RedisGeoCommands.GeoLocation<Object>> item : geoResults.getContent()) {
                    String riderIdStr = String.valueOf(item.getContent().getName());
                    Long riderId = Long.parseLong(riderIdStr);
                    double distKm = item.getDistance().getValue();

                    // Check freshness and availability in metadata hash
                    Optional<RiderTelemetryDto> telemetryOpt = getRiderLocation(riderId);
                    if (telemetryOpt.isPresent()) {
                        RiderTelemetryDto telemetry = telemetryOpt.get();
                        if (telemetry.isFresh()) {
                            results.add(NearbyRiderDto.builder()
                                    .riderId(riderId)
                                    .latitude(telemetry.getLatitude())
                                    .longitude(telemetry.getLongitude())
                                    .distanceKm(distKm)
                                    .isFresh(true)
                                    .ageSeconds(telemetry.getAgeSeconds())
                                    .build());
                        }
                    }
                }
                log.debug("Redis GEO search found {} fresh riders within {}km of ({}, {})",
                        results.size(), radiusKm, centerLat, centerLng);
                return results;
            }
        } catch (Exception ex) {
            log.warn("Redis GEO radius query failed: {}. Falling back to MySQL proximity search.", ex.getMessage());
        }

        // 2. Graceful Fallback: Query MySQL for online & available riders
        List<DeliveryPartner> onlinePartners = partnerRepository.findByStatusAndIsOnlineTrue(DeliveryPartnerStatus.AVAILABLE);
        for (DeliveryPartner partner : onlinePartners) {
            Double pLat = partner.getCurrentLat();
            Double pLng = partner.getCurrentLng();

            if (pLat != null && pLng != null && RiderTelemetryDto.isValidCoordinate(pLat, pLng)) {
                double distKm = calculateHaversineDistanceKm(centerLat, centerLng, pLat, pLng);
                if (distKm <= radiusKm) {
                    results.add(NearbyRiderDto.builder()
                            .riderId(partner.getId())
                            .latitude(pLat)
                            .longitude(pLng)
                            .distanceKm(distKm)
                            .isFresh(false)
                            .ageSeconds(120L)
                            .build());
                }
            }
        }

        results.sort(Comparator.comparingDouble(NearbyRiderDto::getDistanceKm));
        return results;
    }

    /**
     * Removes a rider from the spatial index when they go offline.
     */
    public void removeRiderLocation(Long riderId) {
        try {
            redisTemplate.opsForGeo().remove(REDIS_GEO_KEY, String.valueOf(riderId));
            redisTemplate.delete(REDIS_META_PREFIX + riderId);
            log.debug("Removed rider #{} from Redis GEO index", riderId);
        } catch (Exception ex) {
            log.warn("Failed to remove rider #{} from Redis: {}", riderId, ex.getMessage());
        }
    }

    private void persistDurableLocation(Long riderId, double lat, double lng, long timestamp) {
        try {
            partnerRepository.findById(riderId).ifPresent(partner -> {
                partner.setCurrentLat(lat);
                partner.setCurrentLng(lng);
                partnerRepository.save(partner);

                DeliveryLocation location = DeliveryLocation.builder()
                        .deliveryPartner(partner)
                        .latitude(lat)
                        .longitude(lng)
                        .build();
                locationRepository.save(location);

                lastDurableWriteTimestamps.put(riderId, timestamp);
                log.debug("Persisted throttled location for rider #{} to MySQL", riderId);
            });
        } catch (Exception ex) {
            log.error("Failed to persist durable location to MySQL for rider #{}: {}", riderId, ex.getMessage());
        }
    }

    private double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
