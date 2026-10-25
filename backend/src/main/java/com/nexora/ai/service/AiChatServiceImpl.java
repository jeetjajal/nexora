package com.nexora.ai.service;

import com.nexora.ai.client.GeminiAiClient;
import com.nexora.ai.client.StubAiClient;
import com.nexora.ai.dto.AiChatRequest;
import com.nexora.ai.dto.AiChatResponse;
import com.nexora.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final GeminiAiClient geminiAiClient;
    private final StubAiClient stubAiClient;

    @Value("${nexora.ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${nexora.ai.provider:gemini}")
    private String preferredProvider;

    @Override
    public AiChatResponse chat(AiChatRequest request, UserPrincipal caller) {
        if (!aiEnabled) {
            return AiChatResponse.builder()
                    .reply("Nexora AI Assistant is currently disabled by administrator configuration.")
                    .intent("DISABLED")
                    .provider("NONE")
                    .success(false)
                    .build();
        }

        String userMessage = request.getMessage();
        List<String> toolsExecuted = new ArrayList<>();
        String reply;
        String activeProvider;

        // Try preferred provider (Gemini) if configured and available
        if ("gemini".equalsIgnoreCase(preferredProvider) && geminiAiClient.isAvailable()) {
            try {
                reply = geminiAiClient.chat(userMessage, request.getHistory(), caller, toolsExecuted);
                activeProvider = geminiAiClient.getProviderName();
            } catch (Exception e) {
                log.warn("Gemini AI client error, falling back to local assistant: {}", e.getMessage());
                reply = stubAiClient.chat(userMessage, request.getHistory(), caller, toolsExecuted);
                activeProvider = "LOCAL_STUB_FALLBACK";
            }
        } else {
            // Local Stub / Offline assistant
            reply = stubAiClient.chat(userMessage, request.getHistory(), caller, toolsExecuted);
            activeProvider = stubAiClient.getProviderName();
        }

        String primaryIntent = !toolsExecuted.isEmpty() ? toolsExecuted.get(0) : "GENERAL_CHAT";

        return AiChatResponse.builder()
                .reply(reply)
                .intent(primaryIntent)
                .toolsExecuted(toolsExecuted)
                .provider(activeProvider)
                .success(true)
                .build();
    }
}
