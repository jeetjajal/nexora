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
public class CreateInventoryBatchRequest {
    private Long productId;
    private Long storeId;
    private String batchNumber;
    private LocalDate mfgDate;
    private LocalDate expiryDate;
    private Integer quantity;
    private BigDecimal unitCost;
    private String temperatureZone; // AMBIENT, CHILLED_2_TO_8, FROZEN_MINUS_18
}
