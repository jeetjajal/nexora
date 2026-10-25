package com.nexora.ai.client;

import com.nexora.ai.dto.AiChatMessage;
import com.nexora.auth.security.UserPrincipal;

import java.util.List;

public interface AiClient {

    /**
     * Sends user prompt with conversational history to LLM provider.
     * Executes function calls against approved backend tools when requested by the model.
     */
    String chat(String userMessage, List<AiChatMessage> history, UserPrincipal caller, List<String> toolsExecutedOut);

    String getProviderName();

    boolean isAvailable();
}
