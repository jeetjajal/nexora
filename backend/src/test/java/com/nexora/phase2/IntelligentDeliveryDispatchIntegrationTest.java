package com.nexora.phase2;

import com.nexora.address.entity.Address;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.delivery.dto.NearbyRiderDto;
import com.nexora.delivery.dto.RiderTelemetryDto;
import com.nexora.delivery.entity.AssignmentStatus;
import com.nexora.delivery.entity.DeliveryAssignment;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.DeliveryPartnerStatus;
import com.nexora.delivery.repository.DeliveryAssignmentRepository;
import com.nexora.delivery.repository.DeliveryLocationRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.delivery.service.DeliveryEtaService;
import com.nexora.delivery.service.DeliveryService;
import com.nexora.delivery.service.RiderLocationService;
import com.nexora.kafka.producer.KafkaEventProducer;
import com.nexora.order.dto.OrderTrackingResponse;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.order.service.OrderService;
import com.nexora.role.entity.RoleName;
import com.nexora.routing.service.RoutingService;
import com.nexora.store.entity.Store;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 2: Intelligent Delivery Dispatch & Order Tracking Integration Tests")
class IntelligentDeliveryDispatchIntegrationTest {

    @Mock private DeliveryPartnerRepository partnerRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private DeliveryLocationRepository locationRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private KafkaEventProducer kafkaEventProducer;
    @Mock private RiderLocationService riderLocationService;
    @Mock private RoutingService routingService;
    @Mock private DeliveryEtaService deliveryEtaService;

    private DeliveryService deliveryService;

    private Store darkStore;
    private Address customerAddress;
    private Order testOrder;
    private DeliveryPartner nearbyRider;
    private DeliveryPartner distantRider;
    private User riderUser;

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryService(
                partnerRepository,
                assignmentRepository,
                locationRepository,
                userRepository,
                orderRepository,
                kafkaEventProducer
        );
        ReflectionTestUtils.setField(deliveryService, "riderLocationService", riderLocationService);
        ReflectionTestUtils.setField(deliveryService, "routingService", routingService);
        ReflectionTestUtils.setField(deliveryService, "deliveryEtaService", deliveryEtaService);

        // Dark Store located in Navrangpura, Ahmedabad (23.0365, 72.5611)
        darkStore = Store.builder()
                .id(10L)
                .name("Nexora Navrangpura Dark Store")
                .latitude(23.0365)
                .longitude(72.5611)
                .serviceRadiusKm(5.0)
                .build();

        // Customer in Satellite, Ahmedabad (23.0300, 72.5200) ~4.5km away
        customerAddress = Address.builder()
                .id(100L)
                .addressLine1("Satellite Road")
                .city("Ahmedabad")
                .latitude(23.0300)
                .longitude(72.5200)
                .build();

        testOrder = Order.builder()
                .id(9001L)
                .store(darkStore)
                .deliveryAddress(customerAddress)
                .status(OrderStatus.CONFIRMED)
                .build();

        riderUser = new User();
        riderUser.setId(301L);
        riderUser.setName("Arjun Rider");
        riderUser.setPhone("+91 99999 11111");

        // Rider A: 0.8km from Dark Store
        nearbyRider = new DeliveryPartner();
        nearbyRider.setId(201L);
        nearbyRider.setUser(riderUser);
        nearbyRider.setStatus(DeliveryPartnerStatus.AVAILABLE);
        nearbyRider.setIsOnline(true);
        nearbyRider.setCurrentLat(23.0400);
        nearbyRider.setCurrentLng(72.5650);

        // Rider B: 4.2km from Dark Store
        distantRider = new DeliveryPartner();
        distantRider.setId(202L);
        distantRider.setStatus(DeliveryPartnerStatus.AVAILABLE);
        distantRider.setIsOnline(true);
        distantRider.setCurrentLat(23.0100);
        distantRider.setCurrentLng(72.5300);
    }

    @Test
    @DisplayName("DeliveryService queries Redis Geospatial and assigns closest rider to dark store pickup")
    void testDispatchUsesRedisGeospatialAndDarkStoreOrigin() {
        // Mock Redis geospatial query returning nearby riders
        NearbyRiderDto riderDto = NearbyRiderDto.builder()
                .riderId(201L)
                .distanceKm(0.8)
                .latitude(23.0400)
                .longitude(72.5650)
                .isFresh(true)
                .build();

        when(riderLocationService.findNearbyAvailableRiders(eq(23.0365), eq(72.5611), eq(5.0)))
                .thenReturn(List.of(riderDto));
        when(partnerRepository.findById(201L)).thenReturn(Optional.of(nearbyRider));

        deliveryService.assignNearestRider(testOrder);

        // Verify assignment saved with nearby rider
        ArgumentCaptor<DeliveryAssignment> assignmentCaptor = ArgumentCaptor.forClass(DeliveryAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        DeliveryAssignment captured = assignmentCaptor.getValue();

        assertNotNull(captured);
        assertEquals(201L, captured.getDeliveryPartner().getId());
        assertEquals(AssignmentStatus.ASSIGNED, captured.getStatus());
        assertEquals(DeliveryPartnerStatus.BUSY, nearbyRider.getStatus());
    }

    @Test
    @DisplayName("DeliveryService aborts dispatch when Dark Store coordinates are missing or (0,0)")
    void testDispatchAbortsOnInvalidStoreCoordinates() {
        Store brokenStore = Store.builder()
                .id(99L)
                .name("Store Without Coordinates")
                .latitude(null)
                .longitude(null)
                .build();

        Order orderWithBrokenStore = Order.builder()
                .id(9002L)
                .store(brokenStore)
                .deliveryAddress(customerAddress)
                .build();

        deliveryService.assignNearestRider(orderWithBrokenStore);

        // No assignment or DB changes should occur
        verifyNoInteractions(assignmentRepository);
        verifyNoInteractions(partnerRepository);
    }

    @Test
    @DisplayName("OrderService getOrderTracking enriches response with Phase 2 delivery windows and live rider GPS")
    void testOrderTrackingEnrichment() {
        OrderService orderService = mock(OrderService.class, CALLS_REAL_METHODS);

        // Set up Order
        User customer = new User();
        customer.setId(77L);
        customer.setEmail("customer@nexora.com");

        testOrder.setUser(customer);
        testOrder.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .id(401L)
                .order(testOrder)
                .deliveryPartner(nearbyRider)
                .status(AssignmentStatus.PICKED_UP)
                .build();

        UserPrincipal principal = new UserPrincipal(customer);

        // Mock repositories on OrderService
        OrderRepository orderRepo = mock(OrderRepository.class);
        DeliveryAssignmentRepository assignRepo = mock(DeliveryAssignmentRepository.class);
        RiderLocationService locService = mock(RiderLocationService.class);
        DeliveryEtaService etaService = mock(DeliveryEtaService.class);

        ReflectionTestUtils.setField(orderService, "orderRepository", orderRepo);
        ReflectionTestUtils.setField(orderService, "deliveryAssignmentRepository", assignRepo);
        ReflectionTestUtils.setField(orderService, "riderLocationService", locService);
        ReflectionTestUtils.setField(orderService, "deliveryEtaService", etaService);

        when(orderRepo.findById(9001L)).thenReturn(Optional.of(testOrder));
        when(assignRepo.findByOrderId(9001L)).thenReturn(Optional.of(assignment));

        // Live telemetry
        RiderTelemetryDto liveTelemetry = RiderTelemetryDto.builder()
                .riderId(201L)
                .latitude(23.0380)
                .longitude(72.5630)
                .isFresh(true)
                .ageSeconds(15L)
                .build();
        when(locService.getRiderLocation(201L)).thenReturn(Optional.of(liveTelemetry));

        // ETA response
        com.nexora.delivery.dto.DeliveryEtaResponse etaResponse = com.nexora.delivery.dto.DeliveryEtaResponse.builder()
                .estimatedDeliveryMinutes(14)
                .deliveryWindowStart(LocalDateTime.now().plusMinutes(12))
                .deliveryWindowEnd(LocalDateTime.now().plusMinutes(18))
                .confidence("HIGH")
                .routingProvider("OSRM")
                .isFallbackEstimate(false)
                .roadDistanceMeters(2100.0)
                .roadDurationSeconds(420)
                .isRiderLocationStale(false)
                .build();

        when(etaService.calculateOrderEta(eq(testOrder), eq(23.0380), eq(72.5630), eq(15L)))
                .thenReturn(etaResponse);

        OrderTrackingResponse tracking = orderService.getOrderTracking(principal, 9001L);

        assertNotNull(tracking);
        assertEquals(9001L, tracking.getOrderId());
        assertEquals("HIGH", tracking.getConfidence());
        assertEquals("OSRM", tracking.getRoutingProvider());
        assertFalse(tracking.getIsFallbackEstimate());
        assertFalse(tracking.getIsRiderLocationStale());
        assertEquals(23.0380, tracking.getRiderLatitude());
        assertEquals(72.5630, tracking.getRiderLongitude());
        assertNotNull(tracking.getDeliveryWindowStart());
        assertNotNull(tracking.getDeliveryWindowEnd());
        assertEquals(2100.0, tracking.getRoadDistanceMeters());
    }
}
