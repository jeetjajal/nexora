package com.nexora.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public class OwnerDashboardDto {
    private BigDecimal dailySales;
    private List<String> topSellingItems;
    private List<String> lowStockAlerts;

    public BigDecimal getDailySales() { return dailySales; }
    public void setDailySales(BigDecimal dailySales) { this.dailySales = dailySales; }
    public List<String> getTopSellingItems() { return topSellingItems; }
    public void setTopSellingItems(List<String> topSellingItems) { this.topSellingItems = topSellingItems; }
    public List<String> getLowStockAlerts() { return lowStockAlerts; }
    public void setLowStockAlerts(List<String> lowStockAlerts) { this.lowStockAlerts = lowStockAlerts; }
}
