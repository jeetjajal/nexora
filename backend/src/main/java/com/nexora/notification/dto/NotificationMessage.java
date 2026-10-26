package com.nexora.notification.dto;

import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.NotificationEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private Long orderId;
    private String recipient;
    private NotificationChannel channel;
    private NotificationEventType eventType;
    private String title;
    private String body;
    private String deduplicationKey;
}
