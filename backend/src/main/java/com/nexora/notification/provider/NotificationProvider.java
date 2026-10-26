package com.nexora.notification.provider;

import com.nexora.notification.dto.NotificationDeliveryResult;
import com.nexora.notification.dto.NotificationMessage;
import com.nexora.notification.entity.NotificationChannel;

public interface NotificationProvider {
    NotificationChannel getSupportedChannel();
    String getProviderName();
    boolean isConfigured();
    NotificationDeliveryResult send(NotificationMessage message);
}
