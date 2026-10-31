package com.nexora.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryBatchResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productCategory;
    private Long storeId;
    private String storeName;
    private String batchNumber;
    private LocalDate mfgDate;
    private LocalDate expiryDate;
    private Long daysUntilExpiry;
    private Integer initialQuantity;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private BigDecimal unitCost;
    private String temperatureZone;
    private Boolean isQuarantined;
    private Integer discountAppliedPercent;
    private Boolean isExpired;
    private String freshnessStatus; // FRESH, NEAR_EXPIRY_48H, CRITICAL_24H, EXPIRED
}
