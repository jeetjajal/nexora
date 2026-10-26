package com.nexora.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private Long storeId;
    private String storeName;
    private Long deliveryAddressId;
    private String status;
    private List<OrderItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private String deliveryOtp;
    private String paymentMethod;
    private String paymentStatus;
    private String assignedRiderName;
    private String assignedRiderPhone;

    // Store operations & picking/packing audit
    private Long pickerId;
    private String pickerName;
    private LocalDateTime pickingStartedAt;
    private LocalDateTime pickingCompletedAt;
    private Long packerId;
    private String packerName;
    private LocalDateTime packingStartedAt;
    private LocalDateTime packingCompletedAt;
    private String packingBagCode;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
