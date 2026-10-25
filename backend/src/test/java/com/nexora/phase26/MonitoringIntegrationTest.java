package com.nexora.phase26;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MonitoringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public endpoint /api/v1/monitoring/metrics is accessible without JWT")
    void testPublicMetricsAccess() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/metrics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestsPerSecond").isNumber())
                .andExpect(jsonPath("$.data.p95LatencyMs").isNumber());
    }

    @Test
    @DisplayName("Actuator endpoints /actuator/info and /actuator/metrics are exposed")
    void testActuatorEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    @DisplayName("Static dashboard /monitoring.html is publicly accessible")
    void testStaticDashboardAccess() throws Exception {
        mockMvc.perform(get("/monitoring.html"))
                .andExpect(status().isOk());
    }
}
