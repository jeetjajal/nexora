package com.nexora.phase26;

import com.nexora.monitoring.dto.GrafanaDashboardSpec;
import com.nexora.monitoring.dto.PrometheusAlertRule;
import com.nexora.monitoring.dto.SystemMetricsSnapshot;
import com.nexora.monitoring.service.MetricsCollectorService;
import com.nexora.monitoring.service.MetricsCollectorServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsCollectorServiceTest {

    private MetricsCollectorService metricsCollectorService;

    @BeforeEach
    void setUp() {
        metricsCollectorService = new MetricsCollectorServiceImpl(new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("Should collect and report real-time SDE metrics snapshot")
    void testGetRealtimeSnapshot() {
        SystemMetricsSnapshot snapshot = metricsCollectorService.getRealtimeSnapshot();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getRequestsPerSecond()).isGreaterThan(0.0);
        assertThat(snapshot.getP95LatencyMs()).isLessThan(snapshot.getP99LatencyMs());
        assertThat(snapshot.getActiveDbConnections()).isLessThanOrEqualTo(snapshot.getMaxDbPoolSize());
        assertThat(snapshot.getRedisCacheHitRatePercent()).isBetween(0.0, 100.0);
        assertThat(snapshot.getKafkaConsumerLagRecords()).isGreaterThanOrEqualTo(0);
        assertThat(snapshot.getPaymentSuccessesLastHour()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should record payment results and cache events dynamically")
    void testRecordMetricsEvents() {
        int initialSuccess = metricsCollectorService.getRealtimeSnapshot().getPaymentSuccessesLastHour();
        metricsCollectorService.recordPaymentResult(true);
        assertThat(metricsCollectorService.getRealtimeSnapshot().getPaymentSuccessesLastHour())
                .isEqualTo(initialSuccess + 1);

        metricsCollectorService.recordCacheEvent(true);
        metricsCollectorService.recordOrderProcessingTime(120);
        assertThat(metricsCollectorService.getRealtimeSnapshot().getOrderProcessingTimeMs())
                .isEqualTo(120.0);
    }

    @Test
    @DisplayName("Should provide Prometheus alert rules and Grafana dashboard specs")
    void testAlertRulesAndGrafana() {
        List<PrometheusAlertRule> alerts = metricsCollectorService.getAlertRules();
        assertThat(alerts).isNotEmpty();
        assertThat(alerts).extracting(PrometheusAlertRule::getAlertName)
                .contains("HighHttpErrorRate", "P99LatencyDegradation", "PaymentFailureSpike");

        GrafanaDashboardSpec grafana = metricsCollectorService.getGrafanaDashboardSpec();
        assertThat(grafana.getPanels()).hasSize(8);
        assertThat(grafana.getUid()).isEqualTo("nexora-prod-overview");
    }
}
