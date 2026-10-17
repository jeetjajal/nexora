package com.nexora.phase22;

import com.nexora.microservice.dto.SagaSimulationRequest;
import com.nexora.microservice.dto.SagaSimulationResult;
import com.nexora.microservice.model.SagaStatus;
import com.nexora.microservice.service.MicroserviceRegistryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 22: Distributed Saga Orchestrator Unit Tests")
class SagaOrchestratorServiceTest {

    private MicroserviceRegistryServiceImpl registryService;

    @BeforeEach
    void setUp() {
        registryService = new MicroserviceRegistryServiceImpl();
    }

    @Test
    @DisplayName("Saga Happy Path: Order -> Inventory -> Payment -> Notification succeeds")
    void testSagaHappyPath() {
        SagaSimulationRequest request = SagaSimulationRequest.builder()
                .customerId(10L)
                .productId(5L)
                .quantity(2)
                .amount(599.0)
                .simulatePaymentFailure(false)
                .build();

        SagaSimulationResult result = registryService.executeCheckoutSaga(request);

        assertNotNull(result);
        assertTrue(result.isOverallSuccess());
        assertEquals(SagaStatus.COMPLETED_SUCCESSFULLY, result.getStatus());
        assertEquals(5, result.getExecutionSteps().size());
        assertTrue(result.getExecutionSteps().stream().allMatch(s -> s.getStatus().equals("SUCCESS")));
        assertFalse(result.getExecutionSteps().stream().anyMatch(s -> s.isCompensating()));
    }

    @Test
    @DisplayName("Saga Failure & Rollback: Payment fails -> triggers compensating transactions to release inventory and cancel order")
    void testSagaCompensatingRollbackOnPaymentFailure() {
        SagaSimulationRequest request = SagaSimulationRequest.builder()
                .customerId(10L)
                .productId(5L)
                .quantity(2)
                .amount(599.0)
                .simulatePaymentFailure(true)
                .build();

        SagaSimulationResult result = registryService.executeCheckoutSaga(request);

        assertNotNull(result);
        assertFalse(result.isOverallSuccess());
        assertEquals(SagaStatus.ROLLED_BACK_FAILED, result.getStatus());
        assertEquals(5, result.getExecutionSteps().size());

        // Step 3 failed
        assertEquals("FAILED", result.getExecutionSteps().get(2).getStatus());

        // Steps 4 & 5 are compensating actions
        assertTrue(result.getExecutionSteps().get(3).isCompensating());
        assertTrue(result.getExecutionSteps().get(4).isCompensating());
        assertEquals("COMPENSATED", result.getExecutionSteps().get(3).getStatus());
        assertEquals("COMPENSATED", result.getExecutionSteps().get(4).getStatus());
        assertTrue(result.getCompensationSummary().contains("COMPENSATING SUCCESS"));
    }
}
