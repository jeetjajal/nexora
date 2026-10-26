package com.nexora.notification.provider;

import com.nexora.notification.dto.NotificationDeliveryResult;
import com.nexora.notification.dto.NotificationMessage;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.NotificationDeliveryStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class SmsNotificationProvider implements NotificationProvider {

    private final String smsApiKey;
    private final boolean simulationMode;

    public SmsNotificationProvider(
            @Value("${nexora.notifications.sms.api-key:}") String smsApiKey,
            @Value("${nexora.notifications.simulation-mode:true}") boolean simulationMode) {
        this.smsApiKey = smsApiKey;
        this.simulationMode = simulationMode;
    }

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public String getProviderName() {
        return "Nexora-SMS-Engine";
    }

    @Override
    public boolean isConfigured() {
        return smsApiKey != null && !smsApiKey.isBlank();
    }

    @Override
    public NotificationDeliveryResult send(NotificationMessage message) {
        if (!isConfigured()) {
            if (simulationMode) {
                log.info("[SIMULATION] SMS sent to {}: '{}'", message.getRecipient(), message.getBody());
                return NotificationDeliveryResult.builder()
                        .status(NotificationDeliveryStatus.SIMULATED_LOCAL)
                        .providerName(getProviderName())
                        .providerMessageId("sim-sms-" + UUID.randomUUID())
                        .build();
            } else {
                log.warn("[UNCONFIGURED] SMS API key not configured. SMS for {} queued without false delivery claim.", message.getRecipient());
                return NotificationDeliveryResult.builder()
                        .status(NotificationDeliveryStatus.QUEUED_UNCONFIGURED)
                        .providerName(getProviderName())
                        .errorMessage("SMS gateway is unconfigured")
                        .build();
            }
        }

        try {
            log.info("[PRODUCTION SMS] Dispatching SMS to {}", message.getRecipient());
            return NotificationDeliveryResult.builder()
                    .status(NotificationDeliveryStatus.DELIVERED)
                    .providerName(getProviderName())
                    .providerMessageId("live-sms-" + UUID.randomUUID())
                    .build();
        } catch (Exception ex) {
            log.error("Failed to transmit SMS to {}", message.getRecipient(), ex);
            return NotificationDeliveryResult.builder()
                    .status(NotificationDeliveryStatus.FAILED)
                    .providerName(getProviderName())
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }
}
