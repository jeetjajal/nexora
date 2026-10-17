package com.nexora.phase17;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexora.order.entity.OrderStatus;
import com.nexora.websocket.controller.DeliveryTrackingController;
import com.nexora.websocket.dto.DeliveryLocationRequest;
import com.nexora.websocket.dto.DeliveryLocationUpdateMessage;
import com.nexora.websocket.dto.OrderStatusUpdateMessage;
import com.nexora.websocket.service.OrderTrackingWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 17 - DeliveryTrackingController Tests")
class DeliveryTrackingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private OrderTrackingWebSocketService webSocketService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DeliveryTrackingController controller = new DeliveryTrackingController(webSocketService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/delivery/location should broadcast GPS telemetry")
    void testUpdateLocation() throws Exception {
        DeliveryLocationRequest request = DeliveryLocationRequest.builder()
                .latitude(23.0300)
                .longitude(72.5800)
                .speedKmh(32.0)
                .distanceRemainingKm(0.8)
                .etaMinutes(4)
                .statusMessage("En route to customer")
                .build();

        mockMvc.perform(post("/api/v1/orders/101/delivery/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(101))
                .andExpect(jsonPath("$.latitude").value(23.0300))
                .andExpect(jsonPath("$.longitude").value(72.5800))
                .andExpect(jsonPath("$.speedKmh").value(32.0))
                .andExpect(jsonPath("$.distanceRemainingKm").value(0.8));

        verify(webSocketService).broadcastDeliveryLocationUpdate(eq(101L), any(DeliveryLocationUpdateMessage.class));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId}/tracking should return snapshot of current tracking state")
    void testGetTrackingSnapshot() throws Exception {
        OrderStatusUpdateMessage statusUpdate = OrderStatusUpdateMessage.builder()
                .orderId(101L)
                .status(OrderStatus.OUT_FOR_DELIVERY)
                .message("Rider is out for delivery")
                .estimatedDeliveryMinutes(8)
                .timestamp(Instant.now())
                .build();

        when(webSocketService.getLatestStatus(101L)).thenReturn(statusUpdate);

        mockMvc.perform(get("/api/v1/orders/101/tracking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(101))
                .andExpect(jsonPath("$.statusUpdate.status").value("OUT_FOR_DELIVERY"))
                .andExpect(jsonPath("$.statusUpdate.message").value("Rider is out for delivery"));
    }
}
