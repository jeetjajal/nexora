package com.nexora.phase19;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.ai.client.GeminiAiClient;
import com.nexora.ai.client.StubAiClient;
import com.nexora.ai.dto.AiChatRequest;
import com.nexora.ai.dto.AiChatResponse;
import com.nexora.ai.service.AiChatServiceImpl;
import com.nexora.ai.tool.OrderAiTools;
import com.nexora.ai.tool.ProductAiTools;
import com.nexora.ai.tool.StoreAiTools;
import com.nexora.auth.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 19: Gemini API Key Security & Redaction Audit")
class GeminiApiKeySecurityTest {

    @Mock
    private OrderAiTools orderAiTools;

    @Mock
    private StoreAiTools storeAiTools;

    @Mock
    private ProductAiTools productAiTools;

    @Mock
    private StubAiClient stubAiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Security: empty API key is marked unavailable and does not attempt network calls")
    void missingApiKey_MarkedUnavailable() {
        GeminiAiClient client = new GeminiAiClient(
                "",
                "gemini-1.5-flash",
                "https://generativelanguage.googleapis.com/v1beta/models",
                orderAiTools,
                storeAiTools,
                productAiTools,
                objectMapper
        );

        assertThat(client.isAvailable()).isFalse();

        com.nexora.user.entity.User mockUser = com.nexora.user.entity.User.builder()
                .id(1L)
                .email("customer@nexora.test")
                .password("pass")
                .roles(Collections.emptySet())
                .build();
        UserPrincipal caller = new UserPrincipal(mockUser);
        List<String> tools = new ArrayList<>();

        assertThatThrownBy(() -> client.chat("hello", List.of(), caller, tools))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gemini API Key is not configured")
                .hasMessageNotContaining("AIza")
                .hasMessageNotContaining("secret");
    }

    @Test
    @DisplayName("Security: test placeholder key is rejected by isAvailable() to prevent accidental test network calls")
    void testPlaceholderKey_MarkedUnavailable() {
        GeminiAiClient client = new GeminiAiClient(
                "test-gemini-key",
                "gemini-1.5-flash",
                "https://generativelanguage.googleapis.com/v1beta/models",
                orderAiTools,
                storeAiTools,
                productAiTools,
                objectMapper
        );

        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Security: error sanitizer redacts configured API key and standard Gemini key patterns")
    void sanitizeErrorMessage_RedactsApiKey() {
        String secretKey = "AIzaSyFakeSecretKeyForTestingSanitizer123";
        GeminiAiClient client = new GeminiAiClient(
                secretKey,
                "gemini-1.5-flash",
                "https://generativelanguage.googleapis.com/v1beta/models",
                orderAiTools,
                storeAiTools,
                productAiTools,
                objectMapper
        );

        String rawError = "403 Forbidden: Request failed with key " + secretKey + " at endpoint /generateContent";
        Object sanitized = ReflectionTestUtils.invokeMethod(client, "sanitizeErrorMessage", rawError);

        assertThat(sanitized).isNotNull();
        assertThat(sanitized.toString()).doesNotContain(secretKey);
        assertThat(sanitized.toString()).contains("[REDACTED_API_KEY]");
    }

    @Test
    @DisplayName("Resilience: missing Gemini key routes safely to local assistant without crashing")
    void missingKey_FallsBackToStubClientCleanly() {
        GeminiAiClient unavailableGemini = new GeminiAiClient(
                "",
                "gemini-1.5-flash",
                "https://generativelanguage.googleapis.com/v1beta/models",
                orderAiTools,
                storeAiTools,
                productAiTools,
                objectMapper
        );

        when(stubAiClient.getProviderName()).thenReturn("LOCAL_STUB");
        when(stubAiClient.chat(any(), anyList(), any(), anyList()))
                .thenReturn("Hello! How can I help you?");

        AiChatServiceImpl service = new AiChatServiceImpl(unavailableGemini, stubAiClient);
        ReflectionTestUtils.setField(service, "aiEnabled", true);
        ReflectionTestUtils.setField(service, "preferredProvider", "gemini");

        com.nexora.user.entity.User mockUser = com.nexora.user.entity.User.builder()
                .id(1L)
                .email("customer@nexora.test")
                .password("pass")
                .roles(Collections.emptySet())
                .build();
        UserPrincipal caller = new UserPrincipal(mockUser);
        AiChatRequest request = new AiChatRequest("hello", List.of());

        AiChatResponse response = service.chat(request, caller);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getProvider()).isEqualTo("LOCAL_STUB");
        assertThat(response.getReply()).isEqualTo("Hello! How can I help you?");
    }
}
