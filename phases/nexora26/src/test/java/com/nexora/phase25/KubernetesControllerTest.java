package com.nexora.phase25;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.security.JwtAuthenticationEntryPoint;
import com.nexora.auth.security.JwtAuthenticationFilter;
import com.nexora.auth.security.JwtService;
import com.nexora.k8s.controller.KubernetesController;
import com.nexora.k8s.dto.HpaSimulationRequest;
import com.nexora.k8s.dto.HpaSimulationResult;
import com.nexora.k8s.dto.K8sResourceSpec;
import com.nexora.k8s.service.KubernetesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KubernetesController.class)
@AutoConfigureMockMvc(addFilters = false)
class KubernetesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KubernetesService kubernetesService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/k8s/resources returns list of deployed resources")
    void testGetResources() throws Exception {
        K8sResourceSpec spec = K8sResourceSpec.builder()
                .kind("Deployment")
                .name("nexora-order-service")
                .replicas(1)
                .status("RUNNING")
                .build();

        when(kubernetesService.getDeployedResources()).thenReturn(Collections.singletonList(spec));

        mockMvc.perform(get("/api/v1/k8s/resources")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("nexora-order-service"));
    }

    @Test
    @DisplayName("POST /api/v1/k8s/hpa/simulate returns computed scaling decision")
    void testSimulateHpa() throws Exception {
        HpaSimulationResult result = HpaSimulationResult.builder()
                .requestedRps(1200)
                .baselinePods(1)
                .scaledPods(3)
                .scalingDecision("SCALE UP")
                .build();

        when(kubernetesService.simulateHpaScaling(any(HpaSimulationRequest.class))).thenReturn(result);

        HpaSimulationRequest req = HpaSimulationRequest.builder().incomingRps(1200).currentPods(1).build();

        mockMvc.perform(post("/api/v1/k8s/hpa/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scaledPods").value(3))
                .andExpect(jsonPath("$.data.scalingDecision").value("SCALE UP"));
    }
}
