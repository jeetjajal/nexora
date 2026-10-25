package com.nexora.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public class CustomerDashboardDto {
    private Long totalOrders;
    private BigDecimal totalSpent;
    private List<String> favoriteStores;

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
    public List<String> getFavoriteStores() { return favoriteStores; }
    public void setFavoriteStores(List<String> favoriteStores) { this.favoriteStores = favoriteStores; }
}
