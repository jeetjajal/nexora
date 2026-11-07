package com.nexora.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTrackingResponse {
    private Long orderId;
    private String status;
    private String storeName;
    private String storeAddress;
    private String deliveryAddress;
    private Integer estimatedDeliveryMinutes;
    private String assignedRiderName;
    private String assignedRiderPhone;
    private String assignedRiderVehicle;
    private String deliveryOtp;
    private List<TrackingTimelineEvent> timeline;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;

    // Phase 2: Intelligent Delivery Engine Additions
    private LocalDateTime deliveryWindowStart;
    private LocalDateTime deliveryWindowEnd;
    private String confidence; // HIGH, MEDIUM, LOW
    private Double roadDistanceMeters;
    private Integer roadDurationSeconds;
    private String routingProvider;
    private Boolean isFallbackEstimate;
    private Double riderLatitude;
    private Double riderLongitude;
    private Long riderLocationFreshnessSeconds;
    private Boolean isRiderLocationStale;
}
