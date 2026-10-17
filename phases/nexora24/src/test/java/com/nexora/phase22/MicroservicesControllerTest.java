package com.nexora.phase22;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.microservice.controller.MicroservicesController;
import com.nexora.microservice.dto.GatewayRouteConfig;
import com.nexora.microservice.dto.MicroserviceDefinition;
import com.nexora.microservice.dto.SagaSimulationRequest;
import com.nexora.microservice.dto.SagaSimulationResult;
import com.nexora.microservice.model.MicroserviceId;
import com.nexora.microservice.model.SagaStatus;
import com.nexora.microservice.service.MicroserviceRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 22: MicroservicesController Unit Tests")
class MicroservicesControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MicroserviceRegistryService registryService;

    @InjectMocks
    private MicroservicesController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/microservices/catalog: returns 200 with catalog")
    void testGetCatalog() throws Exception {
        MicroserviceDefinition def = MicroserviceDefinition.builder()
                .id(MicroserviceId.API_GATEWAY)
                .name("API Gateway")
                .defaultPort(8080)
                .responsibility("Reverse proxy")
                .build();

        when(registryService.getServiceCatalog()).thenReturn(List.of(def));

        mockMvc.perform(get("/api/v1/microservices/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("API Gateway"))
                .andExpect(jsonPath("$[0].defaultPort").value(8080));
    }

    @Test
    @DisplayName("GET /api/v1/microservices/routes: returns 200 with routes")
    void testGetRoutes() throws Exception {
        GatewayRouteConfig route = GatewayRouteConfig.builder()
                .routeId("order-route")
                .pathPattern("/api/v1/orders/**")
                .targetServiceUri("http://localhost:8084")
                .rateLimitRps(100)
                .build();

        when(registryService.getGatewayRoutes()).thenReturn(List.of(route));

        mockMvc.perform(get("/api/v1/microservices/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].routeId").value("order-route"));
    }

    @Test
    @DisplayName("GET /api/v1/microservices/health: returns 200 with health map")
    void testGetHealth() throws Exception {
        when(registryService.getHealthStatusMap()).thenReturn(Map.of("API Gateway", "UP"));

        mockMvc.perform(get("/api/v1/microservices/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['API Gateway']").value("UP"));
    }

    @Test
    @DisplayName("POST /api/v1/microservices/saga/simulate: returns 200 with saga result")
    void testSimulateSaga() throws Exception {
        SagaSimulationResult result = SagaSimulationResult.builder()
                .sagaId("SAGA-TEST-123")
                .status(SagaStatus.COMPLETED_SUCCESSFULLY)
                .overallSuccess(true)
                .executionSteps(List.of())
                .summary("Success")
                .build();

        when(registryService.executeCheckoutSaga(any(SagaSimulationRequest.class))).thenReturn(result);

        SagaSimulationRequest request = SagaSimulationRequest.builder()
                .customerId(1L)
                .productId(1L)
                .quantity(1)
                .amount(299.0)
                .build();

        mockMvc.perform(post("/api/v1/microservices/saga/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sagaId").value("SAGA-TEST-123"))
                .andExpect(jsonPath("$.overallSuccess").value(true));
    }
}
