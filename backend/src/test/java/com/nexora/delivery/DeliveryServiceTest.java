package com.nexora.delivery;

import com.nexora.delivery.dto.DeliveryPartnerProfileResponse;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.DeliveryPartnerStatus;
import com.nexora.delivery.repository.DeliveryAssignmentRepository;
import com.nexora.delivery.repository.DeliveryLocationRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.delivery.service.DeliveryService;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import com.nexora.store.entity.Store;
import com.nexora.address.entity.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeliveryServiceTest {

    @Mock
    private DeliveryPartnerRepository partnerRepository;
    
    @Mock
    private DeliveryAssignmentRepository assignmentRepository;
    
    @Mock
    private DeliveryLocationRepository locationRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private com.nexora.kafka.producer.KafkaEventProducer kafkaEventProducer;

    @InjectMocks
    private DeliveryService deliveryService;

    private User testUser;
    private DeliveryPartner testPartner;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Rider One")
                .build();
                
        testPartner = DeliveryPartner.builder()
                .id(100L)
                .user(testUser)
                .isOnline(true)
                .status(DeliveryPartnerStatus.AVAILABLE)
                .currentLat(10.0)
                .currentLng(10.0)
                .totalEarnings(BigDecimal.ZERO)
                .build();
    }

    @Test
    void toggleOnlineStatus_success() {
        when(partnerRepository.findByUserId(1L)).thenReturn(Optional.of(testPartner));
        when(partnerRepository.save(any(DeliveryPartner.class))).thenReturn(testPartner);

        DeliveryPartnerProfileResponse response = deliveryService.toggleOnlineStatus(1L, false);

        assertFalse(response.getIsOnline());
        assertEquals(DeliveryPartnerStatus.OFFLINE, response.getStatus());
        verify(partnerRepository).save(testPartner);
    }

    @Test
    void assignNearestRider_choosesNearestToDarkStore() {
        // Dark Store origin coordinates (Ahmedabad Central Hub)
        Store store = Store.builder()
                .id(1L)
                .name("Ahmedabad Central Hub")
                .latitude(23.0225)
                .longitude(72.5714)
                .build();

        // Customer delivery address (far away)
        Address address = new Address();
        address.setLatitude(23.0800);
        address.setLongitude(72.6000);
        
        Order order = new Order();
        order.setId(50L);
        order.setStore(store);
        order.setDeliveryAddress(address);
        
        // Partner 1: Near Dark Store (23.0230, 72.5720) ~80m away from dark store
        DeliveryPartner partner1 = DeliveryPartner.builder()
                .id(1L)
                .status(DeliveryPartnerStatus.AVAILABLE)
                .isOnline(true)
                .currentLat(23.0230)
                .currentLng(72.5720)
                .build();

        // Partner 2: Far from Dark Store (23.0800, 72.5900) ~7km from dark store, near customer
        DeliveryPartner partner2 = DeliveryPartner.builder()
                .id(2L)
                .status(DeliveryPartnerStatus.AVAILABLE)
                .isOnline(true)
                .currentLat(23.0800)
                .currentLng(72.5900)
                .build();
        
        when(partnerRepository.findByStatusAndIsOnlineTrue(DeliveryPartnerStatus.AVAILABLE))
                .thenReturn(List.of(partner1, partner2));
                
        deliveryService.assignNearestRider(order);
        
        // Partner 1 must be chosen because they are closest to dark store pickup
        assertEquals(DeliveryPartnerStatus.BUSY, partner1.getStatus());
        assertEquals(OrderStatus.RIDER_ASSIGNED, order.getStatus());
        verify(assignmentRepository).save(any());
        verify(orderRepository).save(order);
    }

    @Test
    void assignNearestRider_skipsZeroZeroCoordinates() {
        Store store = Store.builder()
                .id(1L)
                .latitude(23.0225)
                .longitude(72.5714)
                .build();

        Order order = new Order();
        order.setId(51L);
        order.setStore(store);

        // Partner 1 has (0.0, 0.0) invalid GPS
        DeliveryPartner partnerInvalidGps = DeliveryPartner.builder()
                .id(1L)
                .status(DeliveryPartnerStatus.AVAILABLE)
                .isOnline(true)
                .currentLat(0.0)
                .currentLng(0.0)
                .build();

        // Partner 2 has real GPS (23.0250, 72.5730)
        DeliveryPartner partnerValidGps = DeliveryPartner.builder()
                .id(2L)
                .status(DeliveryPartnerStatus.AVAILABLE)
                .isOnline(true)
                .currentLat(23.0250)
                .currentLng(72.5730)
                .build();

        when(partnerRepository.findByStatusAndIsOnlineTrue(DeliveryPartnerStatus.AVAILABLE))
                .thenReturn(List.of(partnerInvalidGps, partnerValidGps));

        deliveryService.assignNearestRider(order);

        // Partner 2 must be chosen, partner 1 skipped
        assertEquals(DeliveryPartnerStatus.AVAILABLE, partnerInvalidGps.getStatus());
        assertEquals(DeliveryPartnerStatus.BUSY, partnerValidGps.getStatus());
        assertEquals(OrderStatus.RIDER_ASSIGNED, order.getStatus());
    }
}
