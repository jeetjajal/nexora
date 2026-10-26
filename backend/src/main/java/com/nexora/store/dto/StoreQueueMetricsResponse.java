package com.nexora.store.dto;

import com.nexora.order.dto.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreQueueMetricsResponse {
    private Long storeId;
    private String storeName;
    private long totalOrders;
    private long confirmedCount;
    private long preparingCount;
    private long readyCount;
    private long outForDeliveryCount;
    private long deliveredCount;
    private long lowStockCount;
    private double averageProcessingMinutes;
    private List<OrderResponse> orders;
}
