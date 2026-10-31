package com.nexora.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColdChainOverviewResponse {
    private Long storeId;
    private String storeName;
    private Integer totalBatches;
    private Integer activeBatches;
    private Integer nearExpiryCount;
    private Integer quarantinedCount;
    private List<StorageUnitHealthDto> units;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageUnitHealthDto {
        private String storageUnit;
        private Double currentTemp;
        private Double targetTemp;
        private Double minSafe;
        private Double maxSafe;
        private Boolean inBreach;
        private String lastUpdated;
    }
}
