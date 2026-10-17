package com.nexora.phase27;

import com.nexora.auth.security.JwtAuthenticationEntryPoint;
import com.nexora.auth.security.JwtAuthenticationFilter;
import com.nexora.auth.security.JwtService;
import com.nexora.loadtesting.controller.LoadTestController;
import com.nexora.loadtesting.dto.BeforeAfterOptimizationReport;
import com.nexora.loadtesting.dto.LoadTestResult;
import com.nexora.loadtesting.dto.LoadTestScenario;
import com.nexora.loadtesting.service.LoadTestingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoadTestController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoadTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoadTestingService loadTestingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/loadtest/scenarios returns available test scenarios")
    void testGetScenarios() throws Exception {
        LoadTestScenario scenario = LoadTestScenario.builder()
                .scenarioId("login")
                .scenarioName("User Login")
                .endpoint("/api/v1/auth/login")
                .httpMethod("POST")
                .build();

        when(loadTestingService.getAllScenarios()).thenReturn(Collections.singletonList(scenario));

        mockMvc.perform(get("/api/v1/loadtest/scenarios")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].scenarioId").value("login"));
    }

    @Test
    @DisplayName("POST /api/v1/loadtest/run executes benchmark simulation")
    void testRunBenchmark() throws Exception {
        LoadTestResult result = LoadTestResult.builder()
                .scenarioId("order_creation")
                .requestsPerSecond(850.0)
                .p95LatencyMs(85.0)
                .build();

        when(loadTestingService.runScenarioLoadTest(anyString(), anyInt(), anyInt())).thenReturn(result);

        mockMvc.perform(post("/api/v1/loadtest/run")
                        .param("scenarioId", "order_creation")
                        .param("virtualUsers", "50")
                        .param("durationSeconds", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestsPerSecond").value(850.0));
    }

    @Test
    @DisplayName("GET /api/v1/loadtest/benchmark/before-after returns optimization comparisons")
    void testGetOptimizationReport() throws Exception {
        BeforeAfterOptimizationReport report = BeforeAfterOptimizationReport.builder()
                .flowName("Catalog Search")
                .beforeThroughputRps(125.0)
                .afterThroughputRps(1450.0)
                .build();

        when(loadTestingService.getOptimizationReports()).thenReturn(Collections.singletonList(report));

        mockMvc.perform(get("/api/v1/loadtest/benchmark/before-after")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].afterThroughputRps").value(1450.0));
    }
}
