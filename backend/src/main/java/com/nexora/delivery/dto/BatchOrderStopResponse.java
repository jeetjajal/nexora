package com.nexora.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOrderStopResponse {
    private Long orderId;
    private String orderNumber;
    private Integer sequenceIndex;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private Double latitude;
    private Double longitude;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
}
