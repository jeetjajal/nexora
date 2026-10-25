package com.nexora.reporting;

import com.nexora.reporting.dto.ReportDto;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ReportingService {
    public ReportDto generateDailyReport(LocalDate date) {
        ReportDto dto = new ReportDto();
        dto.setDate(date);
        // Dummy data ? replace with real queries later
        dto.setTotalOrders(123L);
        dto.setTotalRevenue(BigDecimal.valueOf(4567.89));
        dto.setNewCustomers(7L);
        return dto;
    }
}
