package com.nexora.dashboard.dto;

import java.util.List;

public class PartnerDashboardDto {
    private Long totalDeliveries;
    private java.math.BigDecimal totalEarnings;
    private Double averageDistance;
    private List<String> recentAssignments;

    public Long getTotalDeliveries() { return totalDeliveries; }
    public void setTotalDeliveries(Long totalDeliveries) { this.totalDeliveries = totalDeliveries; }
    public java.math.BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(java.math.BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }
    public Double getAverageDistance() { return averageDistance; }
    public void setAverageDistance(Double averageDistance) { this.averageDistance = averageDistance; }
    public List<String> getRecentAssignments() { return recentAssignments; }
    public void setRecentAssignments(List<String> recentAssignments) { this.recentAssignments = recentAssignments; }
}
