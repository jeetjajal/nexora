package com.nexora.monitoring.service;

import com.nexora.monitoring.dto.GrafanaDashboardSpec;
import com.nexora.monitoring.dto.PrometheusAlertRule;
import com.nexora.monitoring.dto.SystemMetricsSnapshot;

import java.util.List;

public interface MetricsCollectorService {
    SystemMetricsSnapshot getRealtimeSnapshot();
    List<PrometheusAlertRule> getAlertRules();
    GrafanaDashboardSpec getGrafanaDashboardSpec();
    void recordOrderProcessingTime(long durationMs);
    void recordPaymentResult(boolean success);
    void recordCacheEvent(boolean hit);
}
