package com.nexora.delivery.service;

import com.nexora.address.entity.Address;
import com.nexora.delivery.dto.DeliveryEtaResponse;
import com.nexora.delivery.dto.RiderTelemetryDto;
import com.nexora.order.entity.Order;
import com.nexora.routing.dto.RouteResponse;
import com.nexora.routing.model.RoutingProfile;
import com.nexora.routing.service.RoutingService;
import com.nexora.store.entity.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * CONFIDENCE-BASED DELIVERY ETA ENGINE (NEXORA 3.0)
 *
 * Computes realistic quick-commerce delivery arrival windows based on:
 * 1. Store prep / picking time (default: 3 minutes)
 * 2. Rider assignment / matching delay (default: 1 minute)
 * 3. Rider transit to dark store (via road network)
 * 4. Road-network transit from store to customer (via OSRM)
 * 5. Doorstep handover buffer (default: 2 minutes)
 *
 * Produces confidence classification (HIGH, MEDIUM, LOW) based on telemetry freshness
 * and road routing provider availability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryEtaService {

    private final RoutingService routingService;

    @Value("${nexora.eta.store-prep-seconds:180}")
    private int storePrepNominalSeconds = 180; // 3.0 mins

    @Value("${nexora.eta.dispatch-delay-seconds:60}")
    private int dispatchDelayNominalSeconds = 60; // 1.0 min

    @Value("${nexora.eta.handover-seconds:120}")
    private int handoverNominalSeconds = 120; // 2.0 mins

    /**
     * Calculates delivery ETA for a given order, leveraging store origin coordinates,
     * customer delivery address, and assigned rider telemetry if available.
     */
    public DeliveryEtaResponse calculateOrderEta(
            Order order, Double riderLat, Double riderLng, Long riderLocationAgeSeconds) {

        Store store = order.getStore();
        Address address = order.getDeliveryAddress();

        if (store == null || store.getLatitude() == null || store.getLongitude() == null ||
                address == null || address.getLatitude() == null || address.getLongitude() == null) {

            // Unresolvable coordinates fallback: conservative 15-minute default window
            LocalDateTime now = LocalDateTime.now();
            return DeliveryEtaResponse.builder()
                    .estimatedDeliveryMinutes(15)
                    .deliveryWindowStart(now.plusMinutes(10))
                    .deliveryWindowEnd(now.plusMinutes(20))
                    .confidence("LOW")
                    .roadDistanceMeters(0.0)
                    .roadDurationSeconds(900)
                    .routingProvider("UNRESOLVED_COORDINATES")
                    .isFallbackEstimate(true)
                    .isRiderLocationStale(true)
                    .calculatedAt(now)
                    .build();
        }

        return calculateDeliveryEta(
                store.getLatitude(), store.getLongitude(),
                address.getLatitude(), address.getLongitude(),
                riderLat, riderLng, riderLocationAgeSeconds
        );
    }

    /**
     * Core ETA formula calculating minimum, expected, and maximum arrival windows.
     */
    public DeliveryEtaResponse calculateDeliveryEta(
            double storeLat, double storeLng,
            double customerLat, double customerLng,
            Double riderLat, Double riderLng,
            Long riderLocationAgeSeconds) {

        LocalDateTime now = LocalDateTime.now();

        // 1. Calculate road routing from dark store to customer delivery address
        RouteResponse storeToCustomerRoute = routingService.calculateRoute(
                storeLat, storeLng, customerLat, customerLng, RoutingProfile.DRIVING);

        double roadDistanceMeters = storeToCustomerRoute.getDistanceMeters();
        int deliveryTransitSeconds = storeToCustomerRoute.getDurationSeconds();
        boolean isRoutingFallback = storeToCustomerRoute.isFallbackEstimate();
        String provider = storeToCustomerRoute.getRoutingProvider();

        // 2. Calculate rider-to-store pickup transit if rider location is known
        int riderPickupTransitSeconds = 0;
        boolean hasRiderCoords = riderLat != null && riderLng != null &&
                RiderTelemetryDto.isValidCoordinate(riderLat, riderLng);

        if (hasRiderCoords) {
            try {
                RouteResponse riderToStoreRoute = routingService.calculateRoute(
                        riderLat, riderLng, storeLat, storeLng, RoutingProfile.DRIVING);
                riderPickupTransitSeconds = riderToStoreRoute.getDurationSeconds();
            } catch (Exception ex) {
                log.debug("Rider pickup route calculation deferred: {}", ex.getMessage());
            }
        }

        // 3. Assemble nominal, minimum, and maximum duration
        int minDurationSeconds = (int) Math.round(
                (storePrepNominalSeconds * 0.7) +
                (dispatchDelayNominalSeconds * 0.5) +
                (deliveryTransitSeconds * 0.9) +
                (handoverNominalSeconds * 0.6)
        );

        int expectedDurationSeconds =
                storePrepNominalSeconds +
                dispatchDelayNominalSeconds +
                riderPickupTransitSeconds +
                deliveryTransitSeconds +
                handoverNominalSeconds;

        int maxDurationSeconds = (int) Math.round(
                (storePrepNominalSeconds * 1.5) +
                (dispatchDelayNominalSeconds * 2.0) +
                (riderPickupTransitSeconds * 1.3) +
                (deliveryTransitSeconds * 1.3) +
                (handoverNominalSeconds * 1.5)
        );

        // Sanity guards (minimum 5 mins, maximum 45 mins for quick commerce)
        minDurationSeconds = Math.max(300, minDurationSeconds);
        expectedDurationSeconds = Math.max(minDurationSeconds + 60, expectedDurationSeconds);
        maxDurationSeconds = Math.max(expectedDurationSeconds + 120, maxDurationSeconds);

        int estimatedDeliveryMinutes = (int) Math.ceil(expectedDurationSeconds / 60.0);
        LocalDateTime windowStart = now.plusSeconds(minDurationSeconds);
        LocalDateTime windowEnd = now.plusSeconds(maxDurationSeconds);

        // 4. Determine Confidence Score
        boolean isStale = riderLocationAgeSeconds != null && riderLocationAgeSeconds > 90L;
        String confidence;

        if (isRoutingFallback || isStale) {
            confidence = "LOW";
        } else if (hasRiderCoords && riderLocationAgeSeconds != null && riderLocationAgeSeconds <= 30L) {
            confidence = "HIGH";
        } else {
            confidence = "MEDIUM";
        }

        return DeliveryEtaResponse.builder()
                .estimatedDeliveryMinutes(estimatedDeliveryMinutes)
                .deliveryWindowStart(windowStart)
                .deliveryWindowEnd(windowEnd)
                .confidence(confidence)
                .roadDistanceMeters(roadDistanceMeters)
                .roadDurationSeconds(deliveryTransitSeconds)
                .routingProvider(provider)
                .isFallbackEstimate(isRoutingFallback)
                .isRiderLocationStale(isStale)
                .calculatedAt(now)
                .build();
    }
}
