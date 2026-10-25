package com.nexora.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Real-time operational metrics snapshot matching Nexora's Phase 26 SDE monitoring requirements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetricsSnapshot {
    // Traffic & Latency
    private double requestsPerSecond;
    private double errorRatePercent;
    private double p50LatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;

    // Host Resources
    private double cpuUsagePercent;
    private double memoryUsagePercent;
    private long jvmUsedHeapMb;
    private long jvmMaxHeapMb;

    // Infrastructure Components
    private int activeDbConnections;
    private int idleDbConnections;
    private int maxDbPoolSize;
    private double redisCacheHitRatePercent;
    private long redisTotalRequests;
    private long kafkaConsumerLagRecords;

    // Business KPIs & Failure Handling
    private int paymentFailuresLastHour;
    private int paymentSuccessesLastHour;
    private double orderProcessingTimeMs;

    private LocalDateTime timestamp;
}
