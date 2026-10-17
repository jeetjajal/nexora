package com.nexora.dashboard;

import com.nexora.dashboard.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // ---------- Customer ----------
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/customer")
    public ResponseEntity<CustomerDashboardDto> getCustomerDashboard() {
        CustomerDashboardDto dto = dashboardService.buildCustomerDashboard();
        return ResponseEntity.ok(dto);
    }

    // ---------- Store Owner ----------
    @PreAuthorize("hasRole('STORE_OWNER')")
    @GetMapping("/owner")
    public ResponseEntity<OwnerDashboardDto> getOwnerDashboard() {
        OwnerDashboardDto dto = dashboardService.buildOwnerDashboard();
        return ResponseEntity.ok(dto);
    }

    // ---------- Delivery Partner ----------
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    @GetMapping("/partner")
    public ResponseEntity<PartnerDashboardDto> getPartnerDashboard() {
        PartnerDashboardDto dto = dashboardService.buildPartnerDashboard();
        return ResponseEntity.ok(dto);
    }

    // ---------- Admin ----------
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<AdminDashboardDto> getAdminDashboard() {
        AdminDashboardDto dto = dashboardService.buildAdminDashboard();
        return ResponseEntity.ok(dto);
    }
}
