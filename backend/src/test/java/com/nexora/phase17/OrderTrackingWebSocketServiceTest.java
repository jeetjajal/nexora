package com.nexora.phase17;

import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.store.entity.Store;
import com.nexora.user.entity.User;
import com.nexora.websocket.dto.DeliveryLocationUpdateMessage;
import com.nexora.websocket.dto.OrderStatusUpdateMessage;
import com.nexora.websocket.service.OrderTrackingWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 17 - OrderTrackingWebSocketService Unit Tests")
class OrderTrackingWebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private OrderTrackingWebSocketService webSocketService;

    @BeforeEach
    void setUp() {
        webSocketService = new OrderTrackingWebSocketService(messagingTemplate);
    }

    @Test
    @DisplayName("Should broadcast order status change to /topic/orders/{orderId}")
    void testBroadcastOrderStatusUpdate() {
        User user = User.builder().id(42L).name("Test Customer").build();
        Store store = Store.builder().id(10L).name("Pizza Hub").build();
        Order order = Order.builder()
                .id(100L)
                .user(user)
                .store(store)
                .status(OrderStatus.CONFIRMED)
                .build();

        webSocketService.broadcastOrderStatusUpdate(order, OrderStatus.PAID, "Order accepted by store");

        ArgumentCaptor<OrderStatusUpdateMessage> messageCaptor = ArgumentCaptor.forClass(OrderStatusUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/100"), messageCaptor.capture());

        OrderStatusUpdateMessage sent = messageCaptor.getValue();
        assertThat(sent.getOrderId()).isEqualTo(100L);
        assertThat(sent.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(sent.getPreviousStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(sent.getMessage()).isEqualTo("Order accepted by store");
        assertThat(sent.getStoreName()).isEqualTo("Pizza Hub");
        assertThat(sent.getEstimatedDeliveryMinutes()).isEqualTo(35);

        // Verify cached state
        OrderStatusUpdateMessage cached = webSocketService.getLatestStatus(100L);
        assertThat(cached).isNotNull();
        assertThat(cached.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should broadcast delivery location telemetry to /topic/orders/{orderId}/delivery")
    void testBroadcastDeliveryLocationUpdate() {
        DeliveryLocationUpdateMessage loc = DeliveryLocationUpdateMessage.builder()
                .orderId(100L)
                .riderId(5L)
                .riderName("Rider Ajay")
                .latitude(23.0225)
                .longitude(72.5714)
                .speedKmh(28.5)
                .distanceRemainingKm(1.1)
                .etaMinutes(5)
                .deliveryStatus("Approaching destination")
                .timestamp(Instant.now())
                .build();

        webSocketService.broadcastDeliveryLocationUpdate(100L, loc);

        verify(messagingTemplate).convertAndSend(eq("/topic/orders/100/delivery"), eq(loc));

        DeliveryLocationUpdateMessage cached = webSocketService.getLatestLocation(100L);
        assertThat(cached).isNotNull();
        assertThat(cached.getLatitude()).isEqualTo(23.0225);
        assertThat(cached.getRiderName()).isEqualTo("Rider Ajay");
    }

    @Test
    @DisplayName("Should handle null orders gracefully without throwing exceptions")
    void testNullOrderSafety() {
        webSocketService.broadcastOrderStatusUpdate(null, null, "Null safety check");
        assertThat(webSocketService.getLatestStatus(999L)).isNull();
    }
}
