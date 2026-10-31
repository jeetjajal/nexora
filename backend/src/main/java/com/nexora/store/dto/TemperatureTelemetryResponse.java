package com.nexora.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureTelemetryResponse {
    private Long id;
    private Long storeId;
    private String storageUnit;
    private Double temperatureCelsius;
    private Double humidityPercentage;
    private Double minSafeTemp;
    private Double maxSafeTemp;
    private Boolean breachDetected;
    private String status; // NORMAL, BREACH_ALERT
    private LocalDateTime recordedAt;
}
