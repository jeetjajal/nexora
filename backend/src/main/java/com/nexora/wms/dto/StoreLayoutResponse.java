package com.nexora.wms.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreLayoutResponse implements Serializable {
    private Long storeId;
    private String storeName;
    private List<StoreZoneDto> zones;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreZoneDto implements Serializable {
        private Long zoneId;
        private String zoneCode;
        private String name;
        private String temperatureType;
        private List<StoreAisleDto> aisles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreAisleDto implements Serializable {
        private Long aisleId;
        private String aisleCode;
        private Integer aisleNumber;
        private String direction;
        private Boolean isRestricted;
        private BigDecimal widthMeters;
        private BigDecimal lengthMeters;
        private List<StoreBinDto> bins;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreBinDto implements Serializable {
        private Long binId;
        private String binCode;
        private String fullLocationCode;
        private Integer traversalSequence;
        private BigDecimal xCoord;
        private BigDecimal yCoord;
        private Integer capacityUnits;
        private String status;
        private Long rackId;
        private String rackCode;
        private Long shelfId;
        private String shelfCode;
        private Integer shelfLevel;
    }
}
