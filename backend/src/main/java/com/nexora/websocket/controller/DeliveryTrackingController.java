package com.nexora.websocket.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.websocket.dto.DeliveryLocationRequest;
import com.nexora.websocket.dto.DeliveryLocationUpdateMessage;
import com.nexora.websocket.dto.OrderStatusUpdateMessage;
import com.nexora.websocket.service.OrderTrackingWebSocketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PHASE 17 & NEXORA 3.0: DELIVERY TRACKING CONTROLLER
 *
 * Provides REST, SSE, and STOMP message endpoints for real-time
 * rider telemetry, order lifecycle updates, and live GPS streaming.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DeliveryTrackingController {

    private final OrderTrackingWebSocketService webSocketService;

    // Rate limiter: riderId -> last update epoch millis (minimum 800ms between pings)
    private final Map<Long, Long> lastRiderUpdateEpoch = new ConcurrentHashMap<>();

    /**
     * Real-time Server-Sent Events (SSE) streaming endpoint.
     * Clients (browser/mobile) connect to this endpoint for continuous live updates.
     * GET /api/v1/orders/{orderId}/tracking/sse
     */
    @GetMapping(value = "/api/v1/orders/{orderId}/tracking/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrderTracking(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = principal != null ? principal.getId() : null;
        log.info("SSE tracking connection requested for order {} by user {}", orderId, userId);
        return webSocketService.registerSseClient(orderId, userId);
    }

    /**
     * REST Endpoint for rider app or simulation to report live GPS coordinates.
     * POST /api/v1/orders/{orderId}/delivery/location
     */
    @PostMapping("/api/v1/orders/{orderId}/delivery/location")
    public ResponseEntity<?> updateLocation(
            @PathVariable Long orderId,
            @Valid @RequestBody DeliveryLocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        // 1. Coordinate Validation
        if (request.getLatitude() == null || request.getLongitude() == null
                || request.getLatitude() < -90.0 || request.getLatitude() > 90.0
                || request.getLongitude() < -180.0 || request.getLongitude() > 180.0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "Invalid GPS coordinates: latitude [-90, 90], longitude [-180, 180] required"
            ));
        }

        // 2. Rate-Limiting: Minimum 800ms between coordinate updates per rider
        Long riderId = (principal != null && principal.getId() != null) ? principal.getId() : 101L;
        long now = System.currentTimeMillis();
        Long lastUpdate = lastRiderUpdateEpoch.get(riderId);
        if (lastUpdate != null && (now - lastUpdate) < 800) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "status", "THROTTLED",
                    "message", "Telemetry update throttled: max 1 request per 800ms"
            ));
        }
        lastRiderUpdateEpoch.put(riderId, now);

        String riderName = (principal != null && principal.getUsername() != null) ? principal.getUsername() : "Nexora Express Rider";

        DeliveryLocationUpdateMessage update = DeliveryLocationUpdateMessage.builder()
                .orderId(orderId)
                .riderId(riderId)
                .riderName(riderName)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .speedKmh(request.getSpeedKmh() != null ? request.getSpeedKmh() : 24.5)
                .distanceRemainingKm(request.getDistanceRemainingKm() != null ? request.getDistanceRemainingKm() : 1.2)
                .etaMinutes(request.getEtaMinutes() != null ? request.getEtaMinutes() : 6)
                .deliveryStatus(request.getStatusMessage() != null ? request.getStatusMessage() : "Rider in transit")
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

        if (request.getLatitude() == null || request.getLongitude() == null) {
            return;
        }

        DeliveryLocationUpdateMessage update = DeliveryLocationUpdateMessage.builder()
                .orderId(orderId)
                .riderId(101L)
                .riderName("Nexora Express Rider")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .speedKmh(request.getSpeedKmh() != null ? request.getSpeedKmh() : 24.5)
                .distanceRemainingKm(request.getDistanceRemainingKm() != null ? request.getDistanceRemainingKm() : 1.2)
                .etaMinutes(request.getEtaMinutes() != null ? request.getEtaMinutes() : 6)
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
        snapshot.put("activeSubscribers", webSocketService.getActiveSubscriberCount(orderId));
        snapshot.put("timestamp", Instant.now());

        return ResponseEntity.ok(snapshot);
    }
}
