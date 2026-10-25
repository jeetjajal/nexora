package com.nexora.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public class AdminDashboardDto {
    private Long totalUsers;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private List<String> orderStatusDistribution;

    public Long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public List<String> getOrderStatusDistribution() { return orderStatusDistribution; }
    public void setOrderStatusDistribution(List<String> orderStatusDistribution) { this.orderStatusDistribution = orderStatusDistribution; }
}
