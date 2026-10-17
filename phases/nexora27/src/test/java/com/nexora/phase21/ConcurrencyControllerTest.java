package com.nexora.phase21;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.concurrency.controller.ConcurrencyController;
import com.nexora.concurrency.dto.ConcurrencySimulationRequest;
import com.nexora.concurrency.dto.ConcurrencySimulationResult;
import com.nexora.concurrency.dto.ConcurrencyStrategyInfoResponse;
import com.nexora.concurrency.dto.FlashSalePurchaseRequest;
import com.nexora.concurrency.dto.FlashSalePurchaseResponse;
import com.nexora.concurrency.model.ConcurrencyStrategy;
import com.nexora.concurrency.service.ConcurrencyService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 21: ConcurrencyController Unit Tests")
class ConcurrencyControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ConcurrencyService concurrencyService;

    @InjectMocks
    private ConcurrencyController concurrencyController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(concurrencyController).build();
    }

    @Test
    @DisplayName("POST /api/v1/concurrency/simulate: runs 100-user simulation and returns results")
    void testSimulateFlashSale() throws Exception {
        ConcurrencySimulationResult result = ConcurrencySimulationResult.builder()
                .productId(1L)
                .strategy(ConcurrencyStrategy.PESSIMISTIC_LOCK)
                .initialStock(1)
                .finalStock(0)
                .totalRequests(100)
                .successfulPurchases(1)
                .failedPurchases(99)
                .totalDurationMs(120)
                .throughputReqPerSec(833.3)
                .oversellingPrevented(true)
                .explanation("Test simulation passed")
                .build();

        when(concurrencyService.simulateConcurrentFlashSale(any(ConcurrencySimulationRequest.class)))
                .thenReturn(result);

        ConcurrencySimulationRequest request = ConcurrencySimulationRequest.builder()
                .productId(1L)
                .initialStock(1)
                .totalThreads(100)
                .strategy(ConcurrencyStrategy.PESSIMISTIC_LOCK)
                .build();

        mockMvc.perform(post("/api/v1/concurrency/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.successfulPurchases").value(1))
                .andExpect(jsonPath("$.failedPurchases").value(99))
                .andExpect(jsonPath("$.finalStock").value(0))
                .andExpect(jsonPath("$.oversellingPrevented").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/concurrency/flash-sale/purchase: executes single purchase")
    void testPurchaseFlashSale() throws Exception {
        FlashSalePurchaseResponse response = FlashSalePurchaseResponse.builder()
                .success(true)
                .productId(1L)
                .remainingStock(0)
                .strategyUsed(ConcurrencyStrategy.PESSIMISTIC_LOCK)
                .message("Purchase successful")
                .build();

        when(concurrencyService.purchaseFlashSale(any(FlashSalePurchaseRequest.class)))
                .thenReturn(response);

        FlashSalePurchaseRequest request = FlashSalePurchaseRequest.builder()
                .productId(1L)
                .quantity(1)
                .strategy(ConcurrencyStrategy.PESSIMISTIC_LOCK)
                .build();

        mockMvc.perform(post("/api/v1/concurrency/flash-sale/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.remainingStock").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/concurrency/strategies: returns strategy descriptions")
    void testGetStrategyComparisons() throws Exception {
        ConcurrencyStrategyInfoResponse info = ConcurrencyStrategyInfoResponse.builder()
                .strategy(ConcurrencyStrategy.PESSIMISTIC_LOCK)
                .name("Pessimistic Locking")
                .mechanism("SELECT ... FOR UPDATE")
                .isolationLevel("READ_COMMITTED")
                .bestUsedFor("Flash sales")
                .tradeOffs("Lock contention")
                .build();

        when(concurrencyService.getStrategyComparisons()).thenReturn(List.of(info));

        mockMvc.perform(get("/api/v1/concurrency/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].strategy").value("PESSIMISTIC_LOCK"))
                .andExpect(jsonPath("$[0].name").value("Pessimistic Locking"));
    }
}
