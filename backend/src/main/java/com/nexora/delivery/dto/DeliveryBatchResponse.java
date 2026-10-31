package com.nexora.delivery.dto;

import com.nexora.delivery.entity.DeliveryBatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryBatchResponse {
    private Long id;
    private String batchCode;
    private Long storeId;
    private String storeName;
    private Long deliveryPartnerId;
    private String riderName;
    private String riderPhone;
    private DeliveryBatchStatus status;
    private Integer orderCount;
    private BigDecimal totalDistanceKm;
    private Integer estimatedDurationMins;
    private List<BatchOrderStopResponse> stops;
    private LocalDateTime dispatchedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
