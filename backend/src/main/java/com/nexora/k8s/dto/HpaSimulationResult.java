package com.nexora.k8s.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HpaSimulationResult {
    private int requestedRps;
    private int baselinePods;
    private int scaledPods;
    private double calculatedCpuPercent;
    private String scalingDecision;
    private String hpaFormulaApplied;
    private String rationale;
}
