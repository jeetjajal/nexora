package com.nexora.loadtesting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeforeAfterOptimizationReport {
    private String flowName;
    private double beforeThroughputRps;
    private double afterThroughputRps;
    private double beforeP95LatencyMs;
    private double afterP95LatencyMs;
    private double beforeErrorRatePercent;
    private double afterErrorRatePercent;
    private String keyOptimizationImplemented;
    private List<String> architecturalTakeaways;
}
