package com.nexora.websocket.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.kafka.constant.KafkaTopicConstants;
import com.nexora.kafka.event.*;
import com.nexora.order.entity.OrderStatus;
import com.nexora.websocket.dto.DeliveryLocationUpdateMessage;
import com.nexora.websocket.dto.OrderStatusUpdateMessage;
import com.nexora.websocket.service.OrderTrackingWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * PHASE 17: KAFKA -> WEBSOCKET EVENT BRIDGE
 *
 * Consumes distributed Kafka domain events and relays them in real-time
 * to subscribed front-end STOMP/WebSocket clients.
 *
 * This provides end-to-end decoupling: asynchronous Kafka event pipeline
 * drives real-time reactive UI updates without tight coupling between
 * microservices and web clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketOrderEventConsumer {

    private final OrderTrackingWebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopicConstants.ORDER_EVENTS_TOPIC, groupId = "nexora-websocket-group")
    public void handleOrderEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            if (root.has("itemsToRestore") || root.has("cancellationReason")) {
                OrderCancelledEvent event = objectMapper.treeToValue(root, OrderCancelledEvent.class);
                broadcastManualStatus(event.getOrderId(), event.getUserId(), OrderStatus.CANCELLED,
                        "Order #" + event.getOrderId() + " was cancelled: " + event.getCancellationReason());
            } else if (root.has("readyAt")) {
                OrderReadyEvent event = objectMapper.treeToValue(root, OrderReadyEvent.class);
                broadcastManualStatus(event.getOrderId(), null, OrderStatus.READY,
                        "Order #" + event.getOrderId() + " is packed and ready for pickup.");
            } else if (root.has("confirmedAt")) {
                OrderConfirmedEvent event = objectMapper.treeToValue(root, OrderConfirmedEvent.class);
                broadcastManualStatus(event.getOrderId(), event.getUserId(), OrderStatus.CONFIRMED,
                        "Store confirmed order #" + event.getOrderId() + " and started preparing.");
            } else if (root.has("items")) {
                OrderCreatedEvent event = objectMapper.treeToValue(root, OrderCreatedEvent.class);
                broadcastManualStatus(event.getOrderId(), event.getUserId(), OrderStatus.CREATED,
                        "Order #" + event.getOrderId() + " received! Total: ₹" + event.getFinalTotal());
            }
        } catch (Exception ex) {
            log.error("Failed to forward Kafka order event to WebSocket: {}", message, ex);
        }
    }

    @KafkaListener(topics = KafkaTopicConstants.DELIVERY_EVENTS_TOPIC, groupId = "nexora-websocket-group")
    public void handleDeliveryEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            if (root.has("deliveredAt")) {
                OrderDeliveredEvent event = objectMapper.treeToValue(root, OrderDeliveredEvent.class);
                broadcastManualStatus(event.getOrderId(), event.getUserId(), OrderStatus.DELIVERED,
                        "Order #" + event.getOrderId() + " delivered successfully!");
            } else if (root.has("assignedAt")) {
                RiderAssignedEvent event = objectMapper.treeToValue(root, RiderAssignedEvent.class);
                broadcastManualStatus(event.getOrderId(), null, OrderStatus.RIDER_ASSIGNED,
                        "Delivery partner " + event.getDeliveryPartnerName() + " assigned to order #" + event.getOrderId());
            }
        } catch (Exception ex) {
            log.error("Failed to forward Kafka delivery event to WebSocket: {}", message, ex);
        }
    }

    private void broadcastManualStatus(Long orderId, Long userId, OrderStatus status, String customMessage) {
        OrderStatusUpdateMessage update = OrderStatusUpdateMessage.builder()
                .orderId(orderId)
                .userId(userId)
                .status(status)
                .message(customMessage)
                .timestamp(Instant.now())
                .build();

        // Broadcast to /topic/orders/{orderId} and /topic/users/{userId}/orders
        webSocketService.broadcastOrderStatusMessage(update);
    }
}
