package com.nexora.phase2;

import com.nexora.address.entity.Address;
import com.nexora.delivery.dto.DeliveryEtaResponse;
import com.nexora.delivery.dto.RiderTelemetryDto;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.service.DeliveryEtaService;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.routing.dto.RouteResponse;
import com.nexora.routing.model.RoutingProfile;
import com.nexora.routing.service.RoutingService;
import com.nexora.store.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 2: WS3 — Delivery ETA & Delivery Window Engine Tests")
class DeliveryEtaServiceTest {

    @Mock private RoutingService routingService;

    private DeliveryEtaService deliveryEtaService;

    private Store mockStore;
    private Order mockOrder;
    private Address mockAddress;
    private DeliveryPartner mockPartner;

    private final double storeLat = 28.6315;
    private final double storeLng = 77.2167;
    private final double customerLat = 28.6420;
    private final double customerLng = 77.2210;
    private final double riderLat = 28.6320;
    private final double riderLng = 77.2170;

    @BeforeEach
    void setUp() {
        deliveryEtaService = new DeliveryEtaService(routingService);

        mockStore = new Store();
        mockStore.setId(1L);
        mockStore.setName("Connaught Place Dark Store");
        mockStore.setLatitude(storeLat);
        mockStore.setLongitude(storeLng);

        mockAddress = new Address();
        mockAddress.setId(201L);
        mockAddress.setLatitude(customerLat);
        mockAddress.setLongitude(customerLng);

        mockPartner = new DeliveryPartner();
        mockPartner.setId(501L);
        mockPartner.setCurrentLat(riderLat);
        mockPartner.setCurrentLng(riderLng);

        mockOrder = new Order();
        mockOrder.setId(1001L);
        mockOrder.setStatus(OrderStatus.CONFIRMED);
        mockOrder.setCreatedAt(LocalDateTime.now());
        mockOrder.setDeliveryAddress(mockAddress);
        mockOrder.setStore(mockStore);
    }

    @Test
    @DisplayName("High confidence ETA calculation when OSRM road routing is active and rider GPS is fresh")
    void testHighConfidenceEtaCalculation() {
        // Mock OSRM road route: 1.8km, 360 seconds (6 minutes)
        RouteResponse osrmRoute = RouteResponse.builder()
                .distanceMeters(1800.0)
                .durationSeconds(360)
                .routingProvider("OSRM")
                .isFallbackEstimate(false)
                .routeStatus("Ok")
                .build();

        when(routingService.calculateRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(RoutingProfile.DRIVING)))
                .thenReturn(osrmRoute);

        // Rider GPS is fresh (15 seconds old)
        Long freshRiderAgeSeconds = 15L;

        DeliveryEtaResponse eta = deliveryEtaService.calculateDeliveryEta(
                storeLat, storeLng, customerLat, customerLng, riderLat, riderLng, freshRiderAgeSeconds);

        assertNotNull(eta);
        assertEquals("HIGH", eta.getConfidence());
        assertEquals("OSRM", eta.getRoutingProvider());
        assertFalse(eta.isFallbackEstimate());
        assertFalse(eta.isRiderLocationStale());

        // Delivery window range verification
        assertTrue(eta.getEstimatedDeliveryMinutes() >= 5);
        assertNotNull(eta.getDeliveryWindowStart());
        assertNotNull(eta.getDeliveryWindowEnd());
        assertTrue(eta.getDeliveryWindowEnd().isAfter(eta.getDeliveryWindowStart()));
    }

    @Test
    @DisplayName("Low confidence ETA calculation when routing falls back to Haversine and GPS is stale")
    void testLowConfidenceEtaOnFallbackAndStaleGps() {
        // Fallback straight-line route
        RouteResponse fallbackRoute = RouteResponse.builder()
                .distanceMeters(2200.0)
                .durationSeconds(480)
                .routingProvider("HAVERSINE_FALLBACK")
                .isFallbackEstimate(true)
                .routeStatus("Ok")
                .build();

        when(routingService.calculateRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(RoutingProfile.DRIVING)))
                .thenReturn(fallbackRoute);

        // Rider telemetry is stale (120 seconds old)
        Long staleRiderAgeSeconds = 120L;

        DeliveryEtaResponse eta = deliveryEtaService.calculateDeliveryEta(
                storeLat, storeLng, customerLat, customerLng, riderLat, riderLng, staleRiderAgeSeconds);

        assertNotNull(eta);
        assertEquals("LOW", eta.getConfidence());
        assertEquals("HAVERSINE_FALLBACK", eta.getRoutingProvider());
        assertTrue(eta.isFallbackEstimate());
        assertTrue(eta.isRiderLocationStale());
    }

    @Test
    @DisplayName("Delivery window bounds satisfy Quick Commerce invariants via calculateOrderEta")
    void testDeliveryWindowBoundsViaOrder() {
        RouteResponse route = RouteResponse.builder()
                .distanceMeters(1200.0)
                .durationSeconds(240) // 4 min
                .routingProvider("OSRM")
                .isFallbackEstimate(false)
                .routeStatus("Ok")
                .build();

        when(routingService.calculateRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(RoutingProfile.DRIVING)))
                .thenReturn(route);

        DeliveryEtaResponse eta = deliveryEtaService.calculateOrderEta(
                mockOrder, riderLat, riderLng, 20L);

        // Quick Commerce standard: delivery window start is after now, end is after start
        assertNotNull(eta);
        assertTrue(eta.getEstimatedDeliveryMinutes() >= 5);
        assertTrue(eta.getEstimatedDeliveryMinutes() <= 35);
        assertTrue(eta.getDeliveryWindowEnd().isAfter(eta.getDeliveryWindowStart()));
    }
}
