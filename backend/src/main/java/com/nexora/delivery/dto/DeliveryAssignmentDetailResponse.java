package com.nexora.delivery.dto;

import com.nexora.delivery.entity.AssignmentStatus;
import com.nexora.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignmentDetailResponse {
    private Long assignmentId;
    private Long orderId;
    private String orderNumber;

    // Dark Store details
    private Long storeId;
    private String storeName;
    private String storeAddress;
    private String storePhone;

    // Customer delivery details
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;

    // Pricing & Payment
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus;

    // Items and bag
    private int itemCount;
    private String packingBagCode;

    // Statuses & Timestamps
    private AssignmentStatus assignmentStatus;
    private OrderStatus orderStatus;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
}
