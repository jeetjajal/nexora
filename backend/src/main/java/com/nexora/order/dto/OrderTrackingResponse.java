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
}
