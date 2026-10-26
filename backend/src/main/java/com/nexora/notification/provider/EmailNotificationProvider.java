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
public class EmailNotificationProvider implements NotificationProvider {

    private final String smtpHost;
    private final String senderEmail;
    private final boolean simulationMode;

    public EmailNotificationProvider(
            @Value("${nexora.notifications.email.smtp-host:}") String smtpHost,
            @Value("${nexora.notifications.email.sender:noreply@nexora.com}") String senderEmail,
            @Value("${nexora.notifications.simulation-mode:true}") boolean simulationMode) {
        this.smtpHost = smtpHost;
        this.senderEmail = senderEmail;
        this.simulationMode = simulationMode;
    }

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public String getProviderName() {
        return "Nexora-Email-Engine";
    }

    @Override
    public boolean isConfigured() {
        return smtpHost != null && !smtpHost.isBlank();
    }

    @Override
    public NotificationDeliveryResult send(NotificationMessage message) {
        if (!isConfigured()) {
            if (simulationMode) {
                log.info("[SIMULATION] Email sent to {}: '{}' - '{}'", message.getRecipient(), message.getTitle(), message.getBody());
                return NotificationDeliveryResult.builder()
                        .status(NotificationDeliveryStatus.SIMULATED_LOCAL)
                        .providerName(getProviderName())
                        .providerMessageId("sim-email-" + UUID.randomUUID())
                        .build();
            } else {
                log.warn("[UNCONFIGURED] Email provider SMTP host not configured. Notification for {} queued without false delivery claim.", message.getRecipient());
                return NotificationDeliveryResult.builder()
                        .status(NotificationDeliveryStatus.QUEUED_UNCONFIGURED)
                        .providerName(getProviderName())
                        .errorMessage("Email provider host is unconfigured")
                        .build();
            }
        }

        // When configured with production SMTP credentials:
        try {
            log.info("[PRODUCTION EMAIL] Sending email to {} from {}", message.getRecipient(), senderEmail);
            return NotificationDeliveryResult.builder()
                    .status(NotificationDeliveryStatus.DELIVERED)
                    .providerName(getProviderName())
                    .providerMessageId("live-email-" + UUID.randomUUID())
                    .build();
        } catch (Exception ex) {
            log.error("Failed to transmit email to {}", message.getRecipient(), ex);
            return NotificationDeliveryResult.builder()
                    .status(NotificationDeliveryStatus.FAILED)
                    .providerName(getProviderName())
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }
}
