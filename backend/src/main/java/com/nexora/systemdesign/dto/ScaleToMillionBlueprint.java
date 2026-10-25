package com.nexora.systemdesign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaleToMillionBlueprint {
    private String targetScale;
    private double dailyActiveUsers;
    private double peakRpsEstimate;
    private Map<String, String> scalingTiers;
    private List<String> databaseScalingTactics;
    private List<String> cachingStrategy;
    private List<String> computeAutoScaling;
    private List<String> storageAndCdnStrategy;
}
