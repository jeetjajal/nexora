package com.nexora.dashboard;

import com.nexora.dashboard.dto.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Arrays;

@Service
public class DashboardService {
    public CustomerDashboardDto buildCustomerDashboard() {
        CustomerDashboardDto dto = new CustomerDashboardDto();
        dto.setTotalOrders(42L);
        dto.setTotalSpent(BigDecimal.valueOf(1234.56));
        dto.setFavoriteStores(Arrays.asList("Store A", "Store B"));
        return dto;
    }
    public OwnerDashboardDto buildOwnerDashboard() {
        OwnerDashboardDto dto = new OwnerDashboardDto();
        dto.setDailySales(BigDecimal.valueOf(2500.00));
        dto.setTopSellingItems(Arrays.asList("Item X", "Item Y"));
        dto.setLowStockAlerts(Arrays.asList("Item Z low stock"));
        return dto;
    }
    public PartnerDashboardDto buildPartnerDashboard() {
        PartnerDashboardDto dto = new PartnerDashboardDto();
        dto.setTotalDeliveries(15L);
        dto.setTotalEarnings(BigDecimal.valueOf(300.00));
        dto.setAverageDistance(2.5);
        dto.setRecentAssignments(Arrays.asList("Order #101", "Order #102"));
        return dto;
    }
    public AdminDashboardDto buildAdminDashboard() {
        AdminDashboardDto dto = new AdminDashboardDto();
        dto.setTotalUsers(120L);
        dto.setTotalOrders(350L);
        dto.setTotalRevenue(BigDecimal.valueOf(50000.00));
        dto.setOrderStatusDistribution(Arrays.asList("READY:50", "DELIVERED:200", "CANCELLED:10"));
        return dto;
    }
}
