package com.nexora.phase17;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexora.order.entity.OrderStatus;
import com.nexora.websocket.consumer.WebSocketOrderEventConsumer;
import com.nexora.websocket.dto.OrderStatusUpdateMessage;
import com.nexora.websocket.service.OrderTrackingWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 17 - WebSocketOrderEventConsumer (Kafka-to-WebSocket Bridge) Tests")
class WebSocketOrderEventConsumerTest {

    @Mock
    private OrderTrackingWebSocketService webSocketService;

    private ObjectMapper objectMapper;
    private WebSocketOrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new WebSocketOrderEventConsumer(webSocketService, objectMapper);
    }

    @Test
    @DisplayName("Should parse Kafka OrderConfirmedEvent and broadcast CONFIRMED via WebSocket")
    void testHandleOrderConfirmedEvent() {
        String json = """
            {
                "orderId": 200,
                "userId": 5,
                "storeId": 2,
                "confirmedAt": "2026-10-17T11:00:00"
            }
            """;

        consumer.handleOrderEvent(json);

        ArgumentCaptor<OrderStatusUpdateMessage> captor = ArgumentCaptor.forClass(OrderStatusUpdateMessage.class);
        verify(webSocketService).broadcastOrderStatusMessage(captor.capture());

        OrderStatusUpdateMessage message = captor.getValue();
        assertThat(message.getOrderId()).isEqualTo(200L);
        assertThat(message.getUserId()).isEqualTo(5L);
        assertThat(message.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(message.getMessage()).contains("Store confirmed order #200");
    }

    @Test
    @DisplayName("Should parse Kafka OrderReadyEvent and broadcast READY via WebSocket")
    void testHandleOrderReadyEvent() {
        String json = """
            {
                "orderId": 201,
                "storeId": 2,
                "readyAt": "2026-10-17T11:15:00"
            }
            """;

        consumer.handleOrderEvent(json);

        ArgumentCaptor<OrderStatusUpdateMessage> captor = ArgumentCaptor.forClass(OrderStatusUpdateMessage.class);
        verify(webSocketService).broadcastOrderStatusMessage(captor.capture());

        OrderStatusUpdateMessage message = captor.getValue();
        assertThat(message.getOrderId()).isEqualTo(201L);
        assertThat(message.getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    @DisplayName("Should parse Kafka OrderCancelledEvent and broadcast CANCELLED via WebSocket")
    void testHandleOrderCancelledEvent() {
        String json = """
            {
                "orderId": 202,
                "userId": 8,
                "cancellationReason": "Customer requested cancellation",
                "cancelledBy": "customerUser"
            }
            """;

        consumer.handleOrderEvent(json);

        ArgumentCaptor<OrderStatusUpdateMessage> captor = ArgumentCaptor.forClass(OrderStatusUpdateMessage.class);
        verify(webSocketService).broadcastOrderStatusMessage(captor.capture());

        OrderStatusUpdateMessage message = captor.getValue();
        assertThat(message.getOrderId()).isEqualTo(202L);
        assertThat(message.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(message.getMessage()).contains("was cancelled: Customer requested cancellation");
    }
}
