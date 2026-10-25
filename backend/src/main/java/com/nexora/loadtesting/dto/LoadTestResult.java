package com.nexora.loadtesting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadTestResult {
    private String scenarioId;
    private String scenarioName;
    private int virtualUsers;
    private int durationSeconds;
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;

    // Latency Distributions
    private double requestsPerSecond;
    private double avgLatencyMs;
    private double p50LatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    private double errorRatePercent;

    // Resource Consumption
    private double cpuUsagePercent;
    private long memoryUsedMb;
    private int activeDbConnections;
    private double redisHitRatePercent;
    private long kafkaLagRecords;

    // Diagnosis & Fixes
    private List<String> bottlenecksIdentified;
    private List<String> optimizationsApplied;
    private LocalDateTime executedAt;
}
