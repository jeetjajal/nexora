package com.nexora.phase22;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.microservice.dto.SagaSimulationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 22: Microservices Endpoints SpringBoot Integration Test")
class MicroservicesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/microservices/catalog: returns all 10 microservices publicly")
    void testGetCatalogPublic() throws Exception {
        mockMvc.perform(get("/api/v1/microservices/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/microservices/routes: returns API Gateway routes publicly")
    void testGetRoutesPublic() throws Exception {
        mockMvc.perform(get("/api/v1/microservices/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].pathPattern").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/microservices/health: returns microservice health map")
    void testGetHealthPublic() throws Exception {
        mockMvc.perform(get("/api/v1/microservices/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['API Gateway']").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/microservices/saga/simulate: executes happy path saga successfully")
    void testSimulateSagaHappyPath() throws Exception {
        SagaSimulationRequest request = SagaSimulationRequest.builder()
                .customerId(1L)
                .productId(1L)
                .quantity(1)
                .amount(350.0)
                .simulatePaymentFailure(false)
                .build();

        mockMvc.perform(post("/api/v1/microservices/saga/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED_SUCCESSFULLY"))
                .andExpect(jsonPath("$.overallSuccess").value(true))
                .andExpect(jsonPath("$.executionSteps.length()").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/microservices/saga/simulate: executes compensating rollback on payment failure")
    void testSimulateSagaCompensatingRollback() throws Exception {
        SagaSimulationRequest request = SagaSimulationRequest.builder()
                .customerId(1L)
                .productId(1L)
                .quantity(1)
                .amount(350.0)
                .simulatePaymentFailure(true)
                .build();

        mockMvc.perform(post("/api/v1/microservices/saga/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ROLLED_BACK_FAILED"))
                .andExpect(jsonPath("$.overallSuccess").value(false))
                .andExpect(jsonPath("$.executionSteps.length()").value(5))
                .andExpect(jsonPath("$.executionSteps[3].compensating").value(true))
                .andExpect(jsonPath("$.executionSteps[4].compensating").value(true));
    }

    @Test
    @DisplayName("GET /microservices.html: dashboard UI is accessible publicly (200 OK)")
    void testDashboardPageAccessible() throws Exception {
        mockMvc.perform(get("/microservices.html"))
                .andExpect(status().isOk());
    }
}
