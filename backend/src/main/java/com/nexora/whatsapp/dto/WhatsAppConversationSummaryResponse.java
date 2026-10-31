package com.nexora.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppConversationSummaryResponse {
    private Long id;
    private String waPhoneNumber;
    private String customerName;
    private String currentState;
    private String preferredLanguage;
    private String lastMessage;
    private LocalDateTime lastActiveAt;
    private Integer messageCount;
}
