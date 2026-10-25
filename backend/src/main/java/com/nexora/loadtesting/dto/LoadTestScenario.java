package com.nexora.loadtesting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadTestScenario {
    private String scenarioId;
    private String scenarioName;
    private String endpoint;
    private String httpMethod;
    private int recommendedVirtualUsers;
    private String description;
}
