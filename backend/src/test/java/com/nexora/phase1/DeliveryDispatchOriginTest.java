package com.nexora.phase1;

import com.nexora.address.entity.Address;
import com.nexora.delivery.entity.DeliveryAssignment;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.DeliveryPartnerStatus;
import com.nexora.delivery.repository.DeliveryAssignmentRepository;
import com.nexora.delivery.repository.DeliveryLocationRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.delivery.service.DeliveryService;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.store.entity.Store;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 1: Delivery Dispatch Origin & GPS Validation Tests")
class DeliveryDispatchOriginTest {

    @Mock private DeliveryPartnerRepository partnerRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private DeliveryLocationRepository locationRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private com.nexora.kafka.producer.KafkaEventProducer kafkaEventProducer;

    @InjectMocks
    private DeliveryService deliveryService;

    private Store navrangpuraStore;
    private Address thaltejCustomerAddress;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Dark Store located in Navrangpura, Ahmedabad (23.0365, 72.5611)
        navrangpuraStore = Store.builder()
                .id(10L)
                .name("Nexora Navrangpura Dark Store")
                .latitude(23.0365)
                .longitude(72.5611)
                .build();

        // Customer located in Thaltej, Ahmedabad (23.0500, 72.5000) ~6.5km away
        thaltejCustomerAddress = new Address();
        thaltejCustomerAddress.setLatitude(23.0500);
        thaltejCustomerAddress.setLongitude(72.5000);

        testOrder = Order.builder()
                .id(8801L)
                .store(navrangpuraStore)
                .deliveryAddress(thaltejCustomerAddress)
                .status(OrderStatus.CONFIRMED)
                .build();
    }

    @Test
    @DisplayName("Workstream 2: Assigns rider closest to Dark Store pickup point, NOT customer destination")
    void testAssignsRiderClosestToDarkStorePickupOrigin() {
        User userA = User.builder().id(11L).name("Rider A (At Dark Store)").build();
        User userB = User.builder().id(12L).name("Rider B (At Customer House)").build();

        // Rider A: Stationed right next to Navrangpura Dark Store (23.0370, 72.5615) -> ~70m from store, ~6.4km from customer
        DeliveryPartner riderNearStore = DeliveryPartner.builder()
                .id(201L)
                .user(userA)
                .isOnline(true)
                .status(DeliveryPartnerStatus.AVAILABLE)
                .currentLat(23.0370)
                .currentLng(72.5615)
                .build();

        // Rider B: Stationed near Thaltej customer house (23.0495, 72.5005) -> ~6.4km from store, ~70m from customer
        DeliveryPartner riderNearCustomer = DeliveryPartner.builder()
                .id(202L)
                .user(userB)
                .isOnline(true)
                .status(DeliveryPartnerStatus.AVAILABLE)
                .currentLat(23.0495)
                .currentLng(72.5005)
                .build();

        when(partnerRepository.findByStatusAndIsOnlineTrue(DeliveryPartnerStatus.AVAILABLE))
                .thenReturn(List.of(riderNearCustomer, riderNearStore));

        deliveryService.assignNearestRider(testOrder);

        // Verification: Rider A MUST be assigned because pickup starts at the store!
        assertEquals(DeliveryPartnerStatus.BUSY, riderNearStore.getStatus());
        assertEquals(DeliveryPartnerStatus.AVAILABLE, riderNearCustomer.getStatus());
        assertEquals(OrderStatus.RIDER_ASSIGNED, testOrder.getStatus());

        ArgumentCaptor<DeliveryAssignment> assignmentCaptor = ArgumentCaptor.forClass(DeliveryAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertEquals(201L, assignmentCaptor.getValue().getDeliveryPartner().getId());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Workstream 2: Strictly rejects and skips riders with invalid (0.0, 0.0) GPS coordinates")
    void testRejectsZeroZeroRiderCoordinates() {
        User ghostUser = User.builder().id(13L).name("Ghost Rider").build();
        User activeUser = User.builder().id(14L).name("Active Rider").build();

        // Rider with invalid GPS (0,0) - e.g. device permissions revoked
        DeliveryPartner riderGhostGps = DeliveryPartner.builder()
                .id(301L)
                .user(ghostUser)
                .isOnline(true)
                .status(DeliveryPartnerStatus.AVAILABLE)
                .currentLat(0.0)
                .currentLng(0.0)
                .build();

        // Rider with real GPS near store
        DeliveryPartner riderValidGps = DeliveryPartner.builder()
                .id(302L)
                .user(activeUser)
                .isOnline(true)
                .status(DeliveryPartnerStatus.AVAILABLE)
                .currentLat(23.0380)
                .currentLng(72.5620)
                .build();

        when(partnerRepository.findByStatusAndIsOnlineTrue(DeliveryPartnerStatus.AVAILABLE))
                .thenReturn(List.of(riderGhostGps, riderValidGps));

        deliveryService.assignNearestRider(testOrder);

        // Ghost rider must remain AVAILABLE (never picked), real GPS rider must be assigned
        assertEquals(DeliveryPartnerStatus.AVAILABLE, riderGhostGps.getStatus());
        assertEquals(DeliveryPartnerStatus.BUSY, riderValidGps.getStatus());
        assertEquals(OrderStatus.RIDER_ASSIGNED, testOrder.getStatus());
    }

    @Test
    @DisplayName("Workstream 2: Uses Ahmedabad regional dark store fallback coordinates if store lat/lng is missing")
    void testAhmedabadRegionalFallbackCoordinatesWhenStoreLacksLatLng() {
        User riderUser = User.builder().id(15L).name("Vastrapur Rider").build();

        // Store with null lat/lng but name containing "Vastrapur"
        Store storeWithoutCoords = Store.builder()
                .id(15L)
                .name("Nexora Vastrapur Dark Store")
                .latitude(null)
                .longitude(null)
                .build();

        Order order = Order.builder()
                .id(8802L)
                .store(storeWithoutCoords)
                .status(OrderStatus.CONFIRMED)
                .build();

        // Rider near Vastrapur Lake (23.0360, 72.5280)
        DeliveryPartner riderVastrapur = DeliveryPartner.builder()
                .id(401L)
                .user(riderUser)
                .isOnline(true)
                .status(DeliveryPartnerStatus.AVAILABLE)
                .currentLat(23.0360)
                .currentLng(72.5280)
                .build();

        when(partnerRepository.findByStatusAndIsOnlineTrue(DeliveryPartnerStatus.AVAILABLE))
                .thenReturn(List.of(riderVastrapur));

        deliveryService.assignNearestRider(order);

        // Should resolve Vastrapur fallback (23.0358, 72.5293) and successfully assign rider
        assertEquals(DeliveryPartnerStatus.BUSY, riderVastrapur.getStatus());
        assertEquals(OrderStatus.RIDER_ASSIGNED, order.getStatus());
    }

    @Test
    @DisplayName("Workstream 2: Aborts assignment cleanly when store and order have completely unresolvable coordinates")
    void testAbortsCleanlyWhenStoreAndOrderCoordinatesUnresolvable() {
        Store unresolvableStore = Store.builder()
                .id(999L)
                .name("Store X")
                .latitude(0.0)
                .longitude(0.0)
                .build();

        Order order = Order.builder()
                .id(8803L)
                .store(unresolvableStore)
                .deliveryAddress(null)
                .status(OrderStatus.CONFIRMED)
                .build();

        deliveryService.assignNearestRider(order);

        // Must abort safely without assigning or throwing exceptions
        verify(partnerRepository, never()).findByStatusAndIsOnlineTrue(any());
        verify(assignmentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }
}
