package com.nexora.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEtaResponse implements Serializable {
    private int estimatedDeliveryMinutes;
    private LocalDateTime deliveryWindowStart;
    private LocalDateTime deliveryWindowEnd;
    private String confidence; // HIGH, MEDIUM, LOW
    private double roadDistanceMeters;
    private int roadDurationSeconds;
    private String routingProvider;
    private boolean isFallbackEstimate;
    private boolean isRiderLocationStale;
    private LocalDateTime calculatedAt;
}
