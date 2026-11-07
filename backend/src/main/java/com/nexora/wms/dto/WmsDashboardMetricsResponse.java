package com.nexora.wms.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsDashboardMetricsResponse implements Serializable {
    private Long storeId;
    private String storeName;
    private int activeDarkStoresCount;
    private int pendingPickingOrdersCount;
    private int inProgressPickingOrdersCount;
    private int activePickersCount;
    private int ordersAwaitingPackingCount;
    private int ordersAwaitingDispatchCount;
    private BigDecimal averagePickingDurationSeconds;
    private BigDecimal averagePackingDurationSeconds;
    private BigDecimal pickingAccuracyPercentage;
    private BigDecimal missingItemRatePercentage;
    private int expiredStockBlockedCount;
}
