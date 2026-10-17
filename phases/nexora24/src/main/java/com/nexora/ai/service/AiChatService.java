package com.nexora.ai.service;

import com.nexora.ai.dto.AiChatRequest;
import com.nexora.ai.dto.AiChatResponse;
import com.nexora.auth.security.UserPrincipal;

public interface AiChatService {

    AiChatResponse chat(AiChatRequest request, UserPrincipal caller);
}
