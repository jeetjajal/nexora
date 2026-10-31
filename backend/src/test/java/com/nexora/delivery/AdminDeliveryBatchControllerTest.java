package com.nexora.delivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexora.admin.controller.AdminDeliveryBatchController;
import com.nexora.delivery.dto.AssignBatchRequest;
import com.nexora.delivery.dto.BatchGenerationRequest;
import com.nexora.delivery.dto.BatchOrderStopResponse;
import com.nexora.delivery.dto.DeliveryBatchResponse;
import com.nexora.delivery.entity.DeliveryBatchStatus;
import com.nexora.delivery.service.DeliveryBatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDeliveryBatchController Standalone MockMvc Tests")
class AdminDeliveryBatchControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private DeliveryBatchingService batchingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        AdminDeliveryBatchController controller = new AdminDeliveryBatchController(batchingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private DeliveryBatchResponse sampleResponse() {
        return DeliveryBatchResponse.builder()
                .id(99L)
                .batchCode("BATCH-1-98765")
                .storeId(1L)
                .storeName("SG Highway Dark Store Hub")
                .deliveryPartnerId(501L)
                .riderName("Vikram Rider")
                .status(DeliveryBatchStatus.ASSIGNED)
                .orderCount(2)
                .totalDistanceKm(new BigDecimal("1.85"))
                .estimatedDurationMins(14)
                .stops(List.of(
                        BatchOrderStopResponse.builder()
                                .orderId(101L)
                                .orderNumber("NX-101")
                                .sequenceIndex(1)
                                .customerName("Pooja Shah")
                                .status("PENDING")
                                .build(),
                        BatchOrderStopResponse.builder()
                                .orderId(102L)
                                .orderNumber("NX-102")
                                .sequenceIndex(2)
                                .customerName("Rahul Patel")
                                .status("PENDING")
                                .build()
                ))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/batches returns list of batches")
    void testListBatches() throws Exception {
        when(batchingService.getAllBatches()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/admin/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].batchCode").value("BATCH-1-98765"))
                .andExpect(jsonPath("$.data[0].orderCount").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/admin/batches/generate generates new clusters")
    void testGenerateBatches() throws Exception {
        when(batchingService.generateBatchesForStore(any(), any(), any()))
                .thenReturn(List.of(sampleResponse()));

        BatchGenerationRequest req = BatchGenerationRequest.builder()
                .storeId(1L)
                .maxBatchSize(3)
                .maxProximityKm(2.5)
                .build();

        mockMvc.perform(post("/api/v1/admin/batches/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].batchCode").value("BATCH-1-98765"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/batches/{id}/assign assigns rider")
    void testAssignBatch() throws Exception {
        when(batchingService.assignBatchToRider(eq(99L), eq(501L))).thenReturn(sampleResponse());

        AssignBatchRequest req = AssignBatchRequest.builder().deliveryPartnerId(501L).build();

        mockMvc.perform(post("/api/v1/admin/batches/99/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.riderName").value("Vikram Rider"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/batches/{id}/dispatch dispatches batch")
    void testDispatchBatch() throws Exception {
        DeliveryBatchResponse dispatched = sampleResponse();
        dispatched.setStatus(DeliveryBatchStatus.IN_TRANSIT);
        when(batchingService.dispatchBatch(99L)).thenReturn(dispatched);

        mockMvc.perform(post("/api/v1/admin/batches/99/dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("IN_TRANSIT"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/batches/{id}/cancel cancels batch")
    void testCancelBatch() throws Exception {
        DeliveryBatchResponse cancelled = sampleResponse();
        cancelled.setStatus(DeliveryBatchStatus.CANCELLED);
        when(batchingService.cancelBatch(99L)).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/admin/batches/99/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
