package com.nexora.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppBotResponse {
    private String replyMessage;
    private String conversationState;
    private String detectedLanguage;
    private Long orderId;
    private String paymentUrl;
    private List<MatchedItemDto> matchedItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedItemDto {
        private Long productId;
        private String productName;
        private Integer quantity;
        private Double unitPrice;
        private Double subtotal;
        private Boolean inStock;
    }
}
