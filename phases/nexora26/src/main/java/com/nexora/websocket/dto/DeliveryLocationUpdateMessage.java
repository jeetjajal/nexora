package com.nexora.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Real-time rider GPS coordinates and delivery progress update.
 * Topic: /topic/orders/{orderId}/delivery
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryLocationUpdateMessage implements Serializable {

    private Long orderId;
    private Long riderId;
    private String riderName;
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Double distanceRemainingKm;
    private Integer etaMinutes;
    private String deliveryStatus;
    private Instant timestamp;
}
