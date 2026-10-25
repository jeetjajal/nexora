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
    void assignNearestRider_choosesNearest() {
        // Customer location: 0, 0
        Address address = new Address();
        address.setLatitude(0.0);
        address.setLongitude(0.0);
        
        Order order = new Order();
        order.setId(50L);
        order.setDeliveryAddress(address);
        
        // Partner 1: 0.1, 0.1 (Near)
        DeliveryPartner partner1 = DeliveryPartner.builder().id(1L).currentLat(0.1).currentLng(0.1).build();
        // Partner 2: 1.0, 1.0 (Far)
        DeliveryPartner partner2 = DeliveryPartner.builder().id(2L).currentLat(1.0).currentLng(1.0).build();
        
        when(partnerRepository.findByStatusAndIsOnlineTrue(DeliveryPartnerStatus.AVAILABLE))
                .thenReturn(List.of(partner1, partner2));
                
        deliveryService.assignNearestRider(order);
        
        assertEquals(DeliveryPartnerStatus.BUSY, partner1.getStatus());
        assertEquals(OrderStatus.RIDER_ASSIGNED, order.getStatus());
        verify(assignmentRepository).save(any());
        verify(orderRepository).save(order);
    }
}
