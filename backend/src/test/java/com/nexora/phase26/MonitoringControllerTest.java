package com.nexora.phase26;

import com.nexora.auth.security.JwtAuthenticationEntryPoint;
import com.nexora.auth.security.JwtAuthenticationFilter;
import com.nexora.auth.security.JwtService;
import com.nexora.monitoring.controller.MonitoringController;
import com.nexora.monitoring.dto.GrafanaDashboardSpec;
import com.nexora.monitoring.dto.PrometheusAlertRule;
import com.nexora.monitoring.dto.SystemMetricsSnapshot;
import com.nexora.monitoring.service.MetricsCollectorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonitoringController.class)
@AutoConfigureMockMvc(addFilters = false)
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsCollectorService metricsCollectorService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/monitoring/metrics returns metrics snapshot")
    void testGetMetricsSnapshot() throws Exception {
        SystemMetricsSnapshot snapshot = SystemMetricsSnapshot.builder()
                .requestsPerSecond(350.0)
                .p95LatencyMs(75.0)
                .p99LatencyMs(180.0)
                .redisCacheHitRatePercent(88.5)
                .build();

        when(metricsCollectorService.getRealtimeSnapshot()).thenReturn(snapshot);

        mockMvc.perform(get("/api/v1/monitoring/metrics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestsPerSecond").value(350.0))
                .andExpect(jsonPath("$.data.p95LatencyMs").value(75.0));
    }

    @Test
    @DisplayName("GET /api/v1/monitoring/alerts returns Prometheus alert rules")
    void testGetAlerts() throws Exception {
        PrometheusAlertRule rule = PrometheusAlertRule.builder()
                .alertName("HighHttpErrorRate")
                .severity("CRITICAL")
                .status("NORMAL")
                .build();

        when(metricsCollectorService.getAlertRules()).thenReturn(Collections.singletonList(rule));

        mockMvc.perform(get("/api/v1/monitoring/alerts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].alertName").value("HighHttpErrorRate"));
    }

    @Test
    @DisplayName("GET /api/v1/monitoring/grafana-config returns dashboard config")
    void testGetGrafanaConfig() throws Exception {
        GrafanaDashboardSpec spec = GrafanaDashboardSpec.builder()
                .title("Nexora Observability")
                .uid("nexora-overview")
                .build();

        when(metricsCollectorService.getGrafanaDashboardSpec()).thenReturn(spec);

        mockMvc.perform(get("/api/v1/monitoring/grafana-config")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Nexora Observability"));
    }
}
