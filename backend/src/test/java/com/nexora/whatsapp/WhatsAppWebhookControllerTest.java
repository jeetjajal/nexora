package com.nexora.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.whatsapp.controller.WhatsAppWebhookController;
import com.nexora.whatsapp.dto.WhatsAppBotResponse;
import com.nexora.whatsapp.dto.WhatsAppConversationSummaryResponse;
import com.nexora.whatsapp.dto.WhatsAppSimulateRequest;
import com.nexora.whatsapp.service.WhatsAppBotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 3 - WhatsAppWebhookController Tests")
class WhatsAppWebhookControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private WhatsAppBotService botService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        WhatsAppWebhookController controller = new WhatsAppWebhookController(botService, objectMapper);
        ReflectionTestUtils.setField(controller, "verifyToken", "nexora_test_token");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/whatsapp/webhook returns challenge on valid token")
    void testWebhookVerificationHandshake() throws Exception {
        mockMvc.perform(get("/api/v1/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "nexora_test_token")
                        .param("hub.challenge", "1155995533"))
                .andExpect(status().isOk())
                .andExpect(content().string("1155995533"));
    }

    @Test
    @DisplayName("GET /api/v1/whatsapp/webhook returns 403 on invalid token")
    void testWebhookVerificationInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong_token")
                        .param("hub.challenge", "1155995533"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/whatsapp/simulate processes simulated customer message")
    void testSimulateCustomerMessage() throws Exception {
        WhatsAppBotResponse mockResp = WhatsAppBotResponse.builder()
                .replyMessage("Gujarati order summary: 2kg Batata")
                .conversationState("AWAITING_CONFIRMATION")
                .detectedLanguage("gu")
                .build();

        when(botService.processInboundMessage(anyString(), anyString(), anyString())).thenReturn(mockResp);

        WhatsAppSimulateRequest req = WhatsAppSimulateRequest.builder()
                .phone("+919876543210")
                .customerName("Jignesh Shah")
                .message("2 kilo batata moko")
                .build();

        mockMvc.perform(post("/api/v1/whatsapp/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.detectedLanguage").value("gu"))
                .andExpect(jsonPath("$.data.conversationState").value("AWAITING_CONFIRMATION"));
    }

    @Test
    @DisplayName("GET /api/v1/whatsapp/conversations returns list of customer sessions")
    void testGetConversations() throws Exception {
        when(botService.getAllConversations()).thenReturn(List.of(
                WhatsAppConversationSummaryResponse.builder()
                        .id(1L)
                        .waPhoneNumber("+919876543210")
                        .customerName("Jignesh Shah")
                        .currentState("IDLE")
                        .preferredLanguage("gu")
                        .lastMessage("Order confirmed")
                        .lastActiveAt(LocalDateTime.now())
                        .messageCount(4)
                        .build()
        ));

        mockMvc.perform(get("/api/v1/whatsapp/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].customerName").value("Jignesh Shah"));
    }
}
