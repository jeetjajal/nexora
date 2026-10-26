package com.nexora.notification.service;

import com.nexora.notification.dto.NotificationDeliveryResult;
import com.nexora.notification.dto.NotificationDto;
import com.nexora.notification.dto.NotificationMessage;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.NotificationDeliveryStatus;
import com.nexora.notification.entity.NotificationEventType;
import com.nexora.notification.entity.NotificationLog;
import com.nexora.notification.provider.NotificationProvider;
import com.nexora.notification.repository.NotificationLogRepository;
import com.nexora.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final List<NotificationProvider> providers;

    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?i)(bearer\\s+[a-zA-Z0-9._-]+|password=[^&\\s]+)");

    /**
     * Backward-compatible helper for legacy callers.
     */
    public void sendEmail(NotificationDto dto) {
        NotificationMessage msg = NotificationMessage.builder()
                .recipient(dto.getTo())
                .channel(NotificationChannel.EMAIL)
                .eventType(NotificationEventType.ORDER_PLACED)
                .title(dto.getSubject())
                .body(dto.getBody())
                .build();
        dispatch(msg);
    }

    /**
     * Core resilient dispatch method with deduplication, retries, and sanitization.
     */
    @Transactional
    public NotificationLog dispatch(NotificationMessage message) {
        String dedupKey = message.getDeduplicationKey();
        if (dedupKey == null || dedupKey.isBlank()) {
            dedupKey = (message.getOrderId() != null ? message.getOrderId() : "gen") + "_" +
                    message.getEventType() + "_" + message.getChannel();
            message.setDeduplicationKey(dedupKey);
        }

        // Deduplication Check
        if (notificationLogRepository.existsByDeduplicationKey(dedupKey)) {
            log.info("Duplicate notification event intercepted and skipped: dedupKey={}", dedupKey);
            return notificationLogRepository.findByDeduplicationKey(dedupKey).orElse(null);
        }

        // Message Sanitization (scrub card numbers, CVVs, tokens)
        String sanitizedBody = sanitize(message.getBody());

        NotificationProvider provider = findProvider(message.getChannel());
        NotificationDeliveryResult result = null;
        int retries = 0;
        int maxRetries = 3;

        while (retries < maxRetries) {
            try {
                if (provider != null) {
                    result = provider.send(message);
                } else {
                    result = NotificationDeliveryResult.builder()
                            .status(NotificationDeliveryStatus.FAILED)
                            .errorMessage("No provider registered for channel " + message.getChannel())
                            .build();
                }

                if (result.getStatus() == NotificationDeliveryStatus.DELIVERED ||
                        result.getStatus() == NotificationDeliveryStatus.SIMULATED_LOCAL ||
                        result.getStatus() == NotificationDeliveryStatus.QUEUED_UNCONFIGURED) {
                    break;
                }
            } catch (Exception ex) {
                log.warn("Delivery attempt {} failed for recipient {}: {}", retries + 1, message.getRecipient(), ex.getMessage());
            }
            retries++;
        }

        if (result == null) {
            result = NotificationDeliveryResult.builder()
                    .status(NotificationDeliveryStatus.FAILED)
                    .errorMessage("Exhausted retries")
                    .build();
        }

        NotificationLog logEntry = NotificationLog.builder()
                .orderId(message.getOrderId())
                .recipient(message.getRecipient())
                .channel(message.getChannel())
                .eventType(message.getEventType())
                .title(message.getTitle())
                .sanitizedBody(sanitizedBody)
                .deliveryStatus(result.getStatus())
                .providerResponse(result.getProviderName() + ": " +
                        (result.getProviderMessageId() != null ? result.getProviderMessageId() : result.getErrorMessage()))
                .deduplicationKey(dedupKey)
                .retryCount(retries)
                .createdAt(Instant.now())
                .deliveredAt(result.getStatus() == NotificationDeliveryStatus.DELIVERED ? Instant.now() : null)
                .build();

        return notificationLogRepository.save(logEntry);
    }

    public void notifyOrderConfirmed(Order order) {
        if (order == null || order.getUser() == null) return;
        String recipient = order.getUser().getEmail();
        String title = "Order Confirmed - Nexora #" + order.getId();
        String body = String.format("Hi %s, your order #%d totaling ₹%.2f has been confirmed and sent to store for packing.",
                order.getUser().getName(), order.getId(), order.getTotalAmount());

        dispatch(NotificationMessage.builder()
                .orderId(order.getId())
                .recipient(recipient)
                .channel(NotificationChannel.EMAIL)
                .eventType(NotificationEventType.ORDER_CONFIRMED)
                .title(title)
                .body(body)
                .build());
    }

    public void notifyPaymentSuccess(Order order, String transactionId) {
        if (order == null || order.getUser() == null) return;
        String title = "Payment Successful - Nexora #" + order.getId();
        String body = String.format("Payment of ₹%.2f received for Order #%d. Transaction ID: %s",
                order.getTotalAmount(), order.getId(), transactionId);

        dispatch(NotificationMessage.builder()
                .orderId(order.getId())
                .recipient(order.getUser().getEmail())
                .channel(NotificationChannel.EMAIL)
                .eventType(NotificationEventType.PAYMENT_SUCCESS)
                .title(title)
                .body(body)
                .build());
    }

    public void notifyOrderPacked(Order order) {
        if (order == null || order.getUser() == null) return;
        String title = "Order Packed & Ready - Nexora #" + order.getId();
        String body = String.format("Your order #%d has been packed at store %s and is awaiting delivery rider pickup.",
                order.getId(), order.getStore() != null ? order.getStore().getName() : "Dark Store");

        dispatch(NotificationMessage.builder()
                .orderId(order.getId())
                .recipient(order.getUser().getEmail())
                .channel(NotificationChannel.EMAIL)
                .eventType(NotificationEventType.ORDER_PACKED)
                .title(title)
                .body(body)
                .build());
    }

    public void notifyOutForDelivery(Order order, String partnerName, String otp) {
        if (order == null || order.getUser() == null) return;
        String title = "Out for Delivery - Nexora #" + order.getId();
        String body = String.format("Your rider %s is on the way! Share delivery OTP %s with rider upon arrival.",
                partnerName, otp);

        dispatch(NotificationMessage.builder()
                .orderId(order.getId())
                .recipient(order.getUser().getEmail())
                .channel(NotificationChannel.EMAIL)
                .eventType(NotificationEventType.OUT_FOR_DELIVERY)
                .title(title)
                .body(body)
                .build());
    }

    public void notifyOrderDelivered(Order order) {
        if (order == null || order.getUser() == null) return;
        String title = "Delivered! - Nexora #" + order.getId();
        String body = String.format("Your Nexora order #%d has been safely delivered in 10 minutes. Enjoy!",
                order.getId());

        dispatch(NotificationMessage.builder()
                .orderId(order.getId())
                .recipient(order.getUser().getEmail())
                .channel(NotificationChannel.EMAIL)
                .eventType(NotificationEventType.ORDER_DELIVERED)
                .title(title)
                .body(body)
                .build());
    }

    public void notifyRefundProcessed(Order order, BigDecimal refundAmount, String reason) {
        if (order == null || order.getUser() == null) return;
        String title = "Refund Processed - Nexora #" + order.getId();
        String body = String.format("A refund of ₹%.2f has been credited for Order #%d. Reason: %s",
                refundAmount, order.getId(), reason);

        dispatch(NotificationMessage.builder()
                .orderId(order.getId())
                .recipient(order.getUser().getEmail())
                .channel(NotificationChannel.EMAIL)
                .eventType(NotificationEventType.REFUND_PROCESSED)
                .title(title)
                .body(body)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getMyNotifications(String recipientEmail) {
        return notificationLogRepository.findByRecipientOrderByCreatedAtDesc(recipientEmail);
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getAllNotifications() {
        return notificationLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public Map<String, Object> getProviderStatus() {
        return providers.stream()
                .collect(Collectors.toMap(
                        NotificationProvider::getProviderName,
                        p -> Map.of(
                                "channel", p.getSupportedChannel().name(),
                                "isConfigured", p.isConfigured()
                        )
                ));
    }

    private NotificationProvider findProvider(NotificationChannel channel) {
        return providers.stream()
                .filter(p -> p.getSupportedChannel() == channel)
                .findFirst()
                .orElse(null);
    }

    private String sanitize(String text) {
        if (text == null) return "";
        String scrubbed = CARD_PATTERN.matcher(text).replaceAll("[REDACTED_CARD]");
        scrubbed = TOKEN_PATTERN.matcher(scrubbed).replaceAll("[REDACTED_SECRET]");
        return scrubbed;
    }
}
