package com.nexora.notification.dto;

import com.nexora.notification.entity.NotificationDeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeliveryResult {
    private NotificationDeliveryStatus status;
    private String providerName;
    private String providerMessageId;
    private String errorMessage;
}
