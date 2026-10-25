package com.nexora.phase19;

import com.nexora.ai.client.GeminiAiClient;
import com.nexora.ai.client.StubAiClient;
import com.nexora.ai.dto.AiChatRequest;
import com.nexora.ai.dto.AiChatResponse;
import com.nexora.ai.service.AiChatServiceImpl;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private GeminiAiClient geminiAiClient;

    @Mock
    private StubAiClient stubAiClient;

    @InjectMocks
    private AiChatServiceImpl aiChatService;

    private UserPrincipal testUser;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .name("Charlie")
                .email("charlie@nexora.test")
                .roles(Set.of())
                .build();
        testUser = new UserPrincipal(user);

        ReflectionTestUtils.setField(aiChatService, "aiEnabled", true);
        ReflectionTestUtils.setField(aiChatService, "preferredProvider", "gemini");
    }

    @Test
    @DisplayName("chat: uses Gemini client when available")
    void chat_GeminiAvailable() {
        when(geminiAiClient.isAvailable()).thenReturn(true);
        when(geminiAiClient.getProviderName()).thenReturn("GEMINI");
        when(geminiAiClient.chat(eq("Where is my order #5?"), any(), eq(testUser), any()))
                .thenReturn("Your order #5 is preparing in the kitchen.");

        AiChatRequest request = AiChatRequest.builder()
                .message("Where is my order #5?")
                .build();

        AiChatResponse response = aiChatService.chat(request, testUser);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getReply()).isEqualTo("Your order #5 is preparing in the kitchen.");
        assertThat(response.getProvider()).isEqualTo("GEMINI");
    }

    @Test
    @DisplayName("chat: falls back to local stub when Gemini client throws exception")
    void chat_GeminiFallbackOnException() {
        when(geminiAiClient.isAvailable()).thenReturn(true);
        when(geminiAiClient.chat(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Gemini quota exceeded"));

        when(stubAiClient.chat(eq("Where is my order #5?"), any(), eq(testUser), any()))
                .thenReturn("Order #5 status: PREPARING");

        AiChatRequest request = AiChatRequest.builder()
                .message("Where is my order #5?")
                .build();

        AiChatResponse response = aiChatService.chat(request, testUser);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getReply()).isEqualTo("Order #5 status: PREPARING");
        assertThat(response.getProvider()).isEqualTo("LOCAL_STUB_FALLBACK");
    }

    @Test
    @DisplayName("chat: returns disabled message when aiEnabled is false")
    void chat_AiDisabled() {
        ReflectionTestUtils.setField(aiChatService, "aiEnabled", false);

        AiChatRequest request = AiChatRequest.builder()
                .message("Hello")
                .build();

        AiChatResponse response = aiChatService.chat(request, testUser);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getIntent()).isEqualTo("DISABLED");
        assertThat(response.getReply()).contains("disabled");
    }
}
