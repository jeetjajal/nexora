package com.nexora.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppSimulateRequest {
    private String phone;
    private String customerName;
    private String message;
    private String language; // en, gu, hi, or auto
}
