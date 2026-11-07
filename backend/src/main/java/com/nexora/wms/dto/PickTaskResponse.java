package com.nexora.wms.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickTaskResponse implements Serializable {
    private Long pickTaskId;
    private String taskCode;
    private Long orderId;
    private Long storeId;
    private Long pickerId;
    private String pickerName;
    private String status;
    private Integer totalItemsCount;
    private Integer pickedItemsCount;
    private BigDecimal estimatedDistanceMeters;
    private Integer estimatedTimeSeconds;
    private String selectedAlgorithm;
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<PickTaskItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PickTaskItemDto implements Serializable {
        private Long pickTaskItemId;
        private Long orderItemId;
        private Long productId;
        private String productName;
        private String sku;
        private String primaryBarcode;
        private Integer requiredQuantity;
        private Integer pickedQuantity;
        private Long binId;
        private String binLocationCode;
        private BigDecimal xCoord;
        private BigDecimal yCoord;
        private Integer sequenceIndex;
        private Long batchId;
        private String batchNumber;
        private LocalDate expiryDate;
        private String temperatureZone;
        private String status;
        private String scannedBarcode;
        private LocalDateTime pickedAt;
    }
}
