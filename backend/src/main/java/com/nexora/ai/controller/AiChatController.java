package com.nexora.ai.controller;

import com.nexora.ai.dto.AiChatRequest;
import com.nexora.ai.dto.AiChatResponse;
import com.nexora.ai.service.AiChatService;
import com.nexora.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Nexora AI Assistant.
 * Requires user authentication: caller principal is automatically passed
 * to safe backend tools for personalized, authorized responses.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserPrincipal caller
    ) {
        AiChatResponse response = aiChatService.chat(request, caller);
        return ResponseEntity.ok(response);
    }
}
