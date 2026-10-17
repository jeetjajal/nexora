package com.nexora.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReportDto {
    private LocalDate date;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private Long newCustomers;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public Long getNewCustomers() { return newCustomers; }
    public void setNewCustomers(Long newCustomers) { this.newCustomers = newCustomers; }
}
