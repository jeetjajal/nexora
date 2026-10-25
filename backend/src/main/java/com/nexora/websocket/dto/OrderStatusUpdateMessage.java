package com.nexora.websocket.dto;

import com.nexora.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Real-time order status update broadcasted to subscribed WebSocket clients.
 * Topic: /topic/orders/{orderId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateMessage implements Serializable {

    private Long orderId;
    private Long userId;
    private Long storeId;
    private String storeName;
    private OrderStatus status;
    private OrderStatus previousStatus;
    private String message;
    private Integer estimatedDeliveryMinutes;
    private Instant timestamp;
}
