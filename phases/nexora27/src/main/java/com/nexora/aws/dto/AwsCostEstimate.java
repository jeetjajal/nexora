package com.nexora.aws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsCostEstimate {
    private String environment;
    private double monthlyTotalUsd;
    private double monthlyTotalInr;
    private Map<String, Double> breakdownByServiceUsd;
    private String costOptimizationAdvice;
}
