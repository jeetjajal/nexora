package com.nexora.phase19;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.ai.controller.AiChatController;
import com.nexora.ai.dto.AiChatRequest;
import com.nexora.ai.dto.AiChatResponse;
import com.nexora.ai.service.AiChatService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiChatControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AiChatService aiChatService;

    @InjectMocks
    private AiChatController aiChatController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiChatController).build();
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat: returns 200 with AI response")
    void chat_Success() throws Exception {
        AiChatResponse response = AiChatResponse.builder()
                .reply("Your order #1 is on the way!")
                .intent("getOrderStatus")
                .toolsExecuted(List.of("getOrderStatus"))
                .provider("LOCAL_STUB")
                .success(true)
                .build();

        when(aiChatService.chat(any(AiChatRequest.class), any())).thenReturn(response);

        AiChatRequest request = AiChatRequest.builder()
                .message("Where is my order #1?")
                .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Your order #1 is on the way!"))
                .andExpect(jsonPath("$.intent").value("getOrderStatus"))
                .andExpect(jsonPath("$.toolsExecuted[0]").value("getOrderStatus"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat: returns 400 when message is blank")
    void chat_BlankMessageBadRequest() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("   ")
                .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
