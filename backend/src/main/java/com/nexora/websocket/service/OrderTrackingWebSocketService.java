package com.nexora.websocket.service;

import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.websocket.dto.DeliveryLocationUpdateMessage;
import com.nexora.websocket.dto.OrderStatusUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PHASE 17: WEBSOCKET ORDER TRACKING SERVICE
 *
 * Dispatches real-time STOMP messages to subscribed clients via
 * SimpMessagingTemplate.
 *
 * Active Destinations:
 *   - /topic/orders/{orderId}             -> Order lifecycle transitions
 *   - /topic/orders/{orderId}/delivery    -> Real-time rider location coordinates
 *   - /topic/users/{userId}/orders        -> User personal order activity feed
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTrackingWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // Cache the most recent status and location for fast retrieval
    private final Map<Long, OrderStatusUpdateMessage> latestStatusCache = new ConcurrentHashMap<>();
    private final Map<Long, DeliveryLocationUpdateMessage> latestLocationCache = new ConcurrentHashMap<>();

    /**
     * Broadcasts an order status change to WebSocket clients.
     */
    public void broadcastOrderStatusUpdate(Order order, OrderStatus previousStatus, String customMessage) {
        if (order == null || order.getId() == null) {
            return;
        }

        Long orderId = order.getId();
        Long userId = order.getUser() != null ? order.getUser().getId() : null;
        Long storeId = order.getStore() != null ? order.getStore().getId() : null;
        String storeName = order.getStore() != null ? order.getStore().getName() : "Nexora Partner";

        String messageText = customMessage != null && !customMessage.isBlank()
                ? customMessage
                : getDefaultStatusMessage(order.getStatus());

        Integer etaMinutes = estimateMinutesForStatus(order.getStatus());

        OrderStatusUpdateMessage message = OrderStatusUpdateMessage.builder()
                .orderId(orderId)
                .userId(userId)
                .storeId(storeId)
                .storeName(storeName)
                .status(order.getStatus())
                .previousStatus(previousStatus)
                .message(messageText)
                .estimatedDeliveryMinutes(etaMinutes)
                .timestamp(Instant.now())
                .build();

        broadcastOrderStatusMessage(message);
    }

    /**
     * Directly broadcasts an OrderStatusUpdateMessage to /topic/orders/{orderId}
     * and /topic/users/{userId}/orders.
     */
    public void broadcastOrderStatusMessage(OrderStatusUpdateMessage message) {
        if (message == null || message.getOrderId() == null) {
            return;
        }

        latestStatusCache.put(message.getOrderId(), message);

        String orderDestination = "/topic/orders/" + message.getOrderId();
        try {
            messagingTemplate.convertAndSend(orderDestination, message);
            log.info("WebSocket broadcast order update sent to: {} | Status: {}", orderDestination, message.getStatus());
        } catch (Exception ex) {
            log.warn("Failed to broadcast WebSocket order update for order {}: {}", message.getOrderId(), ex.getMessage());
        }

        if (message.getUserId() != null) {
            String userDestination = "/topic/users/" + message.getUserId() + "/orders";
            try {
                messagingTemplate.convertAndSend(userDestination, message);
            } catch (Exception ex) {
                log.debug("Failed to send user channel WebSocket update: {}", ex.getMessage());
            }
        }
    }

    /**
     * Broadcasts rider GPS location updates for a specific order.
     */
    public void broadcastDeliveryLocationUpdate(Long orderId, DeliveryLocationUpdateMessage location) {
        if (orderId == null || location == null) {
            return;
        }

        latestLocationCache.put(orderId, location);

        String destination = "/topic/orders/" + orderId + "/delivery";
        try {
            messagingTemplate.convertAndSend(destination, location);
            log.debug("WebSocket broadcast delivery location sent to: {} | Lat: {}, Lng: {}",
                    destination, location.getLatitude(), location.getLongitude());
        } catch (Exception ex) {
            log.warn("Failed to broadcast WebSocket location for order {}: {}", orderId, ex.getMessage());
        }
    }

    public OrderStatusUpdateMessage getLatestStatus(Long orderId) {
        return latestStatusCache.get(orderId);
    }

    public DeliveryLocationUpdateMessage getLatestLocation(Long orderId) {
        return latestLocationCache.get(orderId);
    }

    private String getDefaultStatusMessage(OrderStatus status) {
        if (status == null) {
            return "Order status updated.";
        }
        return switch (status) {
            case CREATED -> "Order has been created and awaits payment.";
            case PAYMENT_PENDING -> "Payment is being processed.";
            case PAID -> "Payment received successfully. Sent to store.";
            case CONFIRMED -> "Store accepted your order and will start cooking soon.";
            case PREPARING -> "The store is actively preparing your items.";
            case READY -> "Order is packed and ready for pickup.";
            case RIDER_ASSIGNED -> "A delivery partner has been assigned to your order.";
            case PICKED_UP -> "Delivery partner picked up your order from the store.";
            case OUT_FOR_DELIVERY -> "Delivery partner is on the way to your address!";
            case DELIVERED -> "Order has been delivered! Enjoy your meal.";
            case CANCELLED -> "Order was cancelled.";
            case REJECTED -> "Order was rejected by the store.";
        };
    }

    private Integer estimateMinutesForStatus(OrderStatus status) {
        if (status == null) return null;
        return switch (status) {
            case CONFIRMED -> 35;
            case PREPARING -> 25;
            case READY -> 20;
            case RIDER_ASSIGNED -> 18;
            case PICKED_UP -> 15;
            case OUT_FOR_DELIVERY -> 10;
            case DELIVERED -> 0;
            case CANCELLED, REJECTED -> 0;
            default -> 40;
        };
    }
}
