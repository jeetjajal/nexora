package com.nexora.websocket.controller;

import com.nexora.order.dto.OrderResponse;
import com.nexora.order.service.OrderService;
import com.nexora.websocket.dto.DeliveryLocationRequest;
import com.nexora.websocket.dto.DeliveryLocationUpdateMessage;
import com.nexora.websocket.dto.OrderStatusUpdateMessage;
import com.nexora.websocket.service.OrderTrackingWebSocketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * PHASE 17: DELIVERY TRACKING CONTROLLER
 *
 * Provides REST endpoints and STOMP message endpoints to update and
 * query real-time order tracking and rider GPS telemetry.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DeliveryTrackingController {

    private final OrderTrackingWebSocketService webSocketService;

    /**
     * REST Endpoint for rider app or simulation to report live GPS coordinates.
     * POST /api/v1/orders/{orderId}/delivery/location
     */
    @PostMapping("/api/v1/orders/{orderId}/delivery/location")
    public ResponseEntity<DeliveryLocationUpdateMessage> updateLocation(
            @PathVariable Long orderId,
            @Valid @RequestBody DeliveryLocationRequest request) {

        DeliveryLocationUpdateMessage update = DeliveryLocationUpdateMessage.builder()
                .orderId(orderId)
                .riderId(101L) // Default rider identifier
                .riderName("Nexora Express Rider")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .speedKmh(request.getSpeedKmh() != null ? request.getSpeedKmh() : 25.0)
                .distanceRemainingKm(request.getDistanceRemainingKm() != null ? request.getDistanceRemainingKm() : 1.2)
                .etaMinutes(request.getEtaMinutes() != null ? request.getEtaMinutes() : 8)
                .deliveryStatus(request.getStatusMessage() != null ? request.getStatusMessage() : "On the way")
                .timestamp(Instant.now())
                .build();

        webSocketService.broadcastDeliveryLocationUpdate(orderId, update);
        return ResponseEntity.ok(update);
    }

    /**
     * STOMP Message Handler: Allows delivery partners to send GPS points
     * directly over open WebSocket connection without REST HTTP overhead.
     * Inbound Destination: /app/orders/{orderId}/location
     */
    @MessageMapping("/orders/{orderId}/location")
    public void handleInboundLocation(
            @DestinationVariable Long orderId,
            @Payload DeliveryLocationRequest request) {

        DeliveryLocationUpdateMessage update = DeliveryLocationUpdateMessage.builder()
                .orderId(orderId)
                .riderId(101L)
                .riderName("Nexora Express Rider")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .speedKmh(request.getSpeedKmh() != null ? request.getSpeedKmh() : 25.0)
                .distanceRemainingKm(request.getDistanceRemainingKm() != null ? request.getDistanceRemainingKm() : 1.2)
                .etaMinutes(request.getEtaMinutes() != null ? request.getEtaMinutes() : 8)
                .deliveryStatus(request.getStatusMessage() != null ? request.getStatusMessage() : "Rider in transit")
                .timestamp(Instant.now())
                .build();

        webSocketService.broadcastDeliveryLocationUpdate(orderId, update);
    }

    /**
     * REST snapshot lookup of the latest tracking state for initial screen load.
     * GET /api/v1/orders/{orderId}/tracking
     */
    @GetMapping("/api/v1/orders/{orderId}/tracking")
    public ResponseEntity<Map<String, Object>> getTrackingSnapshot(@PathVariable Long orderId) {
        OrderStatusUpdateMessage status = webSocketService.getLatestStatus(orderId);
        DeliveryLocationUpdateMessage location = webSocketService.getLatestLocation(orderId);

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("orderId", orderId);
        snapshot.put("statusUpdate", status);
        snapshot.put("locationUpdate", location);
        snapshot.put("timestamp", Instant.now());

        return ResponseEntity.ok(snapshot);
    }
}
