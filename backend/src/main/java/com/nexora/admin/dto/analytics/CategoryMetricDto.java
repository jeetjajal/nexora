package com.nexora.admin.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMetricDto {
    private Long categoryId;
    private String categoryName;
    private long unitsSold;
    private BigDecimal grossRevenue;
}
