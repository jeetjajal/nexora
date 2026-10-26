package com.nexora.admin.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreFulfillmentMetricDto {
    private Long storeId;
    private String storeName;
    private String city;
    private long orderCount;
    private BigDecimal gmv;
    private double slaCompliancePercent;
    private long avgDeliveryTimeSeconds;
    private long avgFulfillmentTimeSeconds;
    private long outOfStockCount;
}
