package com.nexora.phase17;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.user.entity.User;
import com.nexora.websocket.controller.DeliveryTrackingController;
import com.nexora.websocket.dto.DeliveryLocationRequest;
import com.nexora.websocket.service.OrderTrackingWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class DeliveryTrackingSseIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private OrderTrackingWebSocketService webSocketService;

    @InjectMocks
    private DeliveryTrackingController deliveryTrackingController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(deliveryTrackingController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should successfully register SSE emitter for live tracking stream")
    void testStreamOrderTrackingSse() throws Exception {
        when(webSocketService.registerSseClient(eq(100L), any()))
                .thenReturn(new SseEmitter(60000L));

        MvcResult result = mockMvc.perform(get("/api/v1/orders/100/tracking/sse")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(result.getResponse());
    }

    @Test
    @DisplayName("Should reject invalid GPS coordinates outside valid bounds")
    void testInvalidGpsCoordinates() throws Exception {
        DeliveryLocationRequest invalidReq = new DeliveryLocationRequest();
        invalidReq.setLatitude(150.0); // Invalid > 90
        invalidReq.setLongitude(72.5);

        mockMvc.perform(post("/api/v1/orders/100/delivery/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    @DisplayName("Should successfully accept valid GPS coordinates and broadcast")
    void testValidGpsCoordinates() throws Exception {
        DeliveryLocationRequest validReq = new DeliveryLocationRequest();
        validReq.setLatitude(23.0373);
        validReq.setLongitude(72.5118);
        validReq.setSpeedKmh(24.5);
        validReq.setStatusMessage("Rider en route");

        mockMvc.perform(post("/api/v1/orders/100/delivery/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(23.0373))
                .andExpect(jsonPath("$.longitude").value(72.5118));
    }
}
