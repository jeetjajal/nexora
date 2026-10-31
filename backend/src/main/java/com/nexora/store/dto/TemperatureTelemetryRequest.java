package com.nexora.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureTelemetryRequest {
    private Long storeId;
    private String storageUnit; // CHILLER_01, FREEZER_01, TRANSIT_COOLBOX_12
    private Double temperatureCelsius;
    private Double humidityPercentage;
}
