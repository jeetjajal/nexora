package com.nexora.phase25;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.k8s.dto.HpaSimulationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KubernetesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Public endpoint /api/v1/k8s/resources is accessible without JWT")
    void testPublicResourcesAccess() throws Exception {
        mockMvc.perform(get("/api/v1/k8s/resources")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Public endpoint /api/v1/k8s/hpa/simulate scales order service to 3 pods under 1200 RPS")
    void testPublicHpaSimulation() throws Exception {
        HpaSimulationRequest request = HpaSimulationRequest.builder()
                .incomingRps(1200)
                .currentPods(1)
                .currentCpuUtilizationPercent(70.0)
                .build();

        mockMvc.perform(post("/api/v1/k8s/hpa/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scaledPods").value(3));
    }

    @Test
    @DisplayName("Static dashboard /kubernetes.html is publicly accessible")
    void testStaticDashboardAccess() throws Exception {
        mockMvc.perform(get("/kubernetes.html"))
                .andExpect(status().isOk());
    }
}
