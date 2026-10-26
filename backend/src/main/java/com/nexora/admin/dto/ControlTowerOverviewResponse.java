package com.nexora.admin.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlTowerOverviewResponse {
    private long totalOrders;
    private long activeOrders;
    private long delayedOrdersCount;
    private long cancelledOrders;
    private long refundedOrders;

    private BigDecimal grossMerchandiseValue;
    private BigDecimal deliveryFeesCollected;
    private BigDecimal netPlatformRevenue;

    private double averageDeliveryTimeMinutes;
    private double slaAdherencePercent;

    private int activeHubsCount;
    private int onlineRidersCount;
    private int activeDisputesCount;

    private List<HubOperationalStatusDto> hubStatusList;
    private List<DelayedOrderAlertDto> delayedOrders;
}
