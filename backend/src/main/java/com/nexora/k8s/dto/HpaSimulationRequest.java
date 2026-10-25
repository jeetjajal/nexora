package com.nexora.k8s.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HpaSimulationRequest {
    private int incomingRps;
    private int currentPods;
    private double currentCpuUtilizationPercent;
}
