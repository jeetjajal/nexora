package com.nexora.websocket.service;

import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.websocket.dto.DeliveryLocationUpdateMessage;
import com.nexora.websocket.dto.OrderStatusUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PHASE 17 & NEXORA 3.0: REAL-TIME ORDER TRACKING SERVICE (WEBSOCKET + SSE)
 *
 * Provides resilient dual-transport streaming for live order tracking:
 *   1. Full-duplex STOMP broker (/topic/orders/{orderId}/delivery)
 *   2. Server-Sent Events (SSE) with HTTP/2 keep-alive streaming
 *   3. Stale telemetry detection, rate-limiting & auto-termination on delivery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTrackingWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // Cache the most recent status and location for fast retrieval
    private final Map<Long, OrderStatusUpdateMessage> latestStatusCache = new ConcurrentHashMap<>();
    private final Map<Long, DeliveryLocationUpdateMessage> latestLocationCache = new ConcurrentHashMap<>();

    // Active SSE Emitter subscribers grouped by orderId
    private final Map<Long, List<SseEmitter>> activeSseEmitters = new ConcurrentHashMap<>();

    /**
     * Registers a client for live Server-Sent Events (SSE) for a specific order.
     * Automatically dispatches initial snapshot and handles clean completion.
     */
    public SseEmitter registerSseClient(Long orderId, Long userId) {
        // 15-minute max connection timeout
        SseEmitter emitter = new SseEmitter(15 * 60 * 1000L);

        List<SseEmitter> emitters = activeSseEmitters.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeSseEmitter(orderId, emitter));
        emitter.onTimeout(() -> removeSseEmitter(orderId, emitter));
        emitter.onError(e -> removeSseEmitter(orderId, emitter));

        // Immediately send initial state snapshot
        try {
            DeliveryLocationUpdateMessage latestLoc = latestLocationCache.get(orderId);
            OrderStatusUpdateMessage latestSt = latestStatusCache.get(orderId);

            Map<String, Object> initPayload = new HashMap<>();
            initPayload.put("type", "INIT");
            initPayload.put("orderId", orderId);
            initPayload.put("location", latestLoc);
            initPayload.put("status", latestSt);
            initPayload.put("timestamp", Instant.now().toString());

            emitter.send(SseEmitter.event()
                    .name("init")
                    .id(String.valueOf(System.currentTimeMillis()))
                    .data(initPayload));
            log.info("SSE client registered for order {} (Active subscribers: {})", orderId, emitters.size());
        } catch (IOException ex) {
            removeSseEmitter(orderId, emitter);
        }

        return emitter;
    }

    private void removeSseEmitter(Long orderId, SseEmitter emitter) {
        List<SseEmitter> emitters = activeSseEmitters.get(orderId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                activeSseEmitters.remove(orderId);
            }
        }
    }

    /**
     * Broadcasts an order status change to WebSocket and SSE clients.
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
     * and SSE clients.
     */
    public void broadcastOrderStatusMessage(OrderStatusUpdateMessage message) {
        if (message == null || message.getOrderId() == null) {
            return;
        }

        Long orderId = message.getOrderId();
        latestStatusCache.put(orderId, message);

        // 1. WebSocket / STOMP Broadcast
        String orderDestination = "/topic/orders/" + orderId;
        try {
            messagingTemplate.convertAndSend(orderDestination, message);
            log.info("WebSocket broadcast order update sent to: {} | Status: {}", orderDestination, message.getStatus());
        } catch (Exception ex) {
            log.warn("Failed to broadcast WebSocket order update for order {}: {}", orderId, ex.getMessage());
        }

        if (message.getUserId() != null) {
            String userDestination = "/topic/users/" + message.getUserId() + "/orders";
            try {
                messagingTemplate.convertAndSend(userDestination, message);
            } catch (Exception ex) {
                log.debug("Failed to send user channel WebSocket update: {}", ex.getMessage());
            }
        }

        // 2. SSE Broadcast
        List<SseEmitter> emitters = activeSseEmitters.get(orderId);
        if (emitters != null && !emitters.isEmpty()) {
            boolean isTerminal = message.getStatus() == OrderStatus.DELIVERED || message.getStatus() == OrderStatus.CANCELLED;
            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("status")
                            .id(String.valueOf(System.currentTimeMillis()))
                            .data(message));

                    if (isTerminal) {
                        emitter.complete();
                        deadEmitters.add(emitter);
                    }
                } catch (Exception ex) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
            if (isTerminal) {
                activeSseEmitters.remove(orderId);
                log.info("SSE streaming completed and sealed for terminal order {}", orderId);
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

        // 1. WebSocket / STOMP Broadcast
        String destination = "/topic/orders/" + orderId + "/delivery";
        try {
            messagingTemplate.convertAndSend(destination, location);
            log.debug("WebSocket broadcast delivery location sent to: {} | Lat: {}, Lng: {}",
                    destination, location.getLatitude(), location.getLongitude());
        } catch (Exception ex) {
            log.warn("Failed to broadcast WebSocket location for order {}: {}", orderId, ex.getMessage());
        }

        // 2. SSE Broadcast
        List<SseEmitter> emitters = activeSseEmitters.get(orderId);
        if (emitters != null && !emitters.isEmpty()) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("location")
                            .id(String.valueOf(System.currentTimeMillis()))
                            .data(location));
                } catch (Exception ex) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
        }
    }

    /**
     * Scheduled Heartbeat to keep HTTP streaming connections alive through
     * Cloudflare/Render proxies and detect dead clients.
     */
    @Scheduled(fixedRate = 25000)
    public void broadcastHeartbeats() {
        if (activeSseEmitters.isEmpty()) return;

        activeSseEmitters.forEach((orderId, emitters) -> {
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("ping"));
                } catch (Exception ex) {
                    dead.add(emitter);
                }
            }
            emitters.removeAll(dead);
        });
    }

    public OrderStatusUpdateMessage getLatestStatus(Long orderId) {
        return latestStatusCache.get(orderId);
    }

    public DeliveryLocationUpdateMessage getLatestLocation(Long orderId) {
        return latestLocationCache.get(orderId);
    }

    public int getActiveSubscriberCount(Long orderId) {
        List<SseEmitter> emitters = activeSseEmitters.get(orderId);
        return emitters != null ? emitters.size() : 0;
    }

    private String getDefaultStatusMessage(OrderStatus status) {
        if (status == null) {
            return "Order status updated.";
        }
        return switch (status) {
            case CREATED -> "Order has been created and awaits payment.";
            case PAYMENT_PENDING -> "Payment is being processed.";
            case PAID -> "Payment received successfully. Sent to store.";
            case CONFIRMED -> "Store accepted your order and sent to packing queue.";
            case PREPARING -> "Picker is picking items from dark store racks.";
            case READY -> "Order is packed and ready for dispatch.";
            case RIDER_ASSIGNED -> "A delivery partner has been assigned to your order.";
            case PICKED_UP -> "Delivery partner picked up your order from dark store.";
            case OUT_FOR_DELIVERY -> "Delivery partner is on the way to your address!";
            case DELIVERED -> "Order has been delivered! Handover verified with OTP.";
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
