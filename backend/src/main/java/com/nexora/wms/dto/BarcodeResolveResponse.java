package com.nexora.wms.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarcodeResolveResponse implements Serializable {
    private Long productId;
    private String productName;
    private String sku;
    private String primaryBarcode;
    private BigDecimal price;
    private String primaryBinCode;
    private Integer availableStock;
    private List<BatchStockDto> availableBatches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchStockDto implements Serializable {
        private Long batchId;
        private String batchNumber;
        private LocalDate expiryDate;
        private Integer availableQuantity;
        private String temperatureZone;
        private Boolean isQuarantined;
    }
}
