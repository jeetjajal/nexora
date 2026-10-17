package com.nexora.monitoring.controller;

import com.nexora.common.ApiResponse;
import com.nexora.monitoring.dto.GrafanaDashboardSpec;
import com.nexora.monitoring.dto.PrometheusAlertRule;
import com.nexora.monitoring.dto.SystemMetricsSnapshot;
import com.nexora.monitoring.service.MetricsCollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MetricsCollectorService metricsCollectorService;

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<SystemMetricsSnapshot>> getMetricsSnapshot() {
        return ResponseEntity.ok(ApiResponse.success(
                "Operational metrics snapshot retrieved successfully",
                metricsCollectorService.getRealtimeSnapshot()
        ));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<PrometheusAlertRule>>> getAlertRules() {
        return ResponseEntity.ok(ApiResponse.success(
                "Prometheus alert rules retrieved successfully",
                metricsCollectorService.getAlertRules()
        ));
    }

    @GetMapping("/grafana-config")
    public ResponseEntity<ApiResponse<GrafanaDashboardSpec>> getGrafanaConfig() {
        return ResponseEntity.ok(ApiResponse.success(
                "Grafana dashboard specification retrieved successfully",
                metricsCollectorService.getGrafanaDashboardSpec()
        ));
    }
}
