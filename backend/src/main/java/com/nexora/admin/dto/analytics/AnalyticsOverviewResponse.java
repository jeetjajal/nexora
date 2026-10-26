package com.nexora.admin.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewResponse {
    private String startDate;
    private String endDate;
    private Long storeId;

    // Financial Definitions
    private long totalOrders;
    private BigDecimal grossMerchandiseValue;
    private BigDecimal netPlatformRevenue;
    private BigDecimal totalDeliveryFees;
    private BigDecimal totalDiscounts;
    private BigDecimal averageOrderValue;

    // Delivery & SLA
    private double slaComplianceRatePercent;
    private long averageDeliveryTimeSeconds;
    private long averageFulfillmentTimeSeconds;

    // Disputes & Cancellations
    private long totalCancellations;
    private long totalDisputes;
    private BigDecimal totalRefundsAmount;

    // Payment Reconciliation
    private Map<String, BigDecimal> paymentReconciliation;

    // Breakdowns
    private List<TopProductDto> topSellingProducts;
    private List<CategoryMetricDto> categoryPerformance;
    private List<StoreFulfillmentMetricDto> storeFulfillmentMetrics;
}
