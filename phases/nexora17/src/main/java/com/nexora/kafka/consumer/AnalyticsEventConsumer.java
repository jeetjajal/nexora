package com.nexora.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.kafka.constant.KafkaTopicConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsEventConsumer {

    private final ObjectMapper objectMapper;

    @Getter
    private final AtomicInteger totalOrdersCreated = new AtomicInteger(0);

    @Getter
    private final AtomicInteger totalOrdersConfirmed = new AtomicInteger(0);

    @Getter
    private final AtomicInteger totalOrdersDelivered = new AtomicInteger(0);

    @Getter
    private final AtomicInteger totalOrdersCancelled = new AtomicInteger(0);

    @Getter
    private final AtomicInteger totalPaymentsCompleted = new AtomicInteger(0);

    @Getter
    private final AtomicInteger totalPaymentsFailed = new AtomicInteger(0);

    @Getter
    private final AtomicReference<BigDecimal> totalGrossMerchandiseValue = new AtomicReference<>(BigDecimal.ZERO);

    @KafkaListener(topics = KafkaTopicConstants.ORDER_EVENTS_TOPIC, groupId = "${spring.kafka.consumer.group-id:nexora-group}")
    public void trackOrderEvents(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (root.has("itemsToRestore") || root.has("cancellationReason")) {
                totalOrdersCancelled.incrementAndGet();
                log.info("[Analytics] Order Cancelled logged. Total cancelled: {}", totalOrdersCancelled.get());
            } else if (root.has("confirmedAt")) {
                totalOrdersConfirmed.incrementAndGet();
                log.info("[Analytics] Order Confirmed logged. Total confirmed: {}", totalOrdersConfirmed.get());
            } else if (root.has("items")) {
                totalOrdersCreated.incrementAndGet();
                if (root.has("finalTotal")) {
                    BigDecimal amount = new BigDecimal(root.get("finalTotal").asText());
                    totalGrossMerchandiseValue.accumulateAndGet(amount, BigDecimal::add);
                }
                log.info("[Analytics] Order Created logged. Total orders: {}, GMV: {}",
                        totalOrdersCreated.get(), totalGrossMerchandiseValue.get());
            }
        } catch (Exception e) {
            log.error("[Analytics] Error tracking order event: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopicConstants.PAYMENT_EVENTS_TOPIC, groupId = "${spring.kafka.consumer.group-id:nexora-group}")
    public void trackPaymentEvents(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (root.has("failureReason") || root.has("errorCode")) {
                totalPaymentsFailed.incrementAndGet();
                log.info("[Analytics] Payment Failed logged. Total failed: {}", totalPaymentsFailed.get());
            } else if (root.has("razorpayPaymentId")) {
                totalPaymentsCompleted.incrementAndGet();
                log.info("[Analytics] Payment Completed logged. Total successful: {}", totalPaymentsCompleted.get());
            }
        } catch (Exception e) {
            log.error("[Analytics] Error tracking payment event: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopicConstants.DELIVERY_EVENTS_TOPIC, groupId = "${spring.kafka.consumer.group-id:nexora-group}")
    public void trackDeliveryEvents(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (root.has("deliveredAt")) {
                totalOrdersDelivered.incrementAndGet();
                log.info("[Analytics] Order Delivered logged. Total delivered: {}", totalOrdersDelivered.get());
            }
        } catch (Exception e) {
            log.error("[Analytics] Error tracking delivery event: {}", message, e);
        }
    }
}
