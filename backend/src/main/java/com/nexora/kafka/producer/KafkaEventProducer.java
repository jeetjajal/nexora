package com.nexora.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.kafka.constant.KafkaTopicConstants;
import com.nexora.kafka.event.*;
import com.nexora.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 1. OrderCreated
    public void publishOrderCreated(OrderCreatedEvent event) {
        sendEvent(KafkaTopicConstants.ORDER_EVENTS_TOPIC, String.valueOf(event.getOrderId()), event, "OrderCreatedEvent");
    }

    public void publishOrderCreated(Order order) {
        List<OrderItemEventDto> items = order.getItems() != null
                ? order.getItems().stream()
                .map(item -> OrderItemEventDto.builder()
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductNameSnapshot())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getSubtotal())
                        .build())
                .collect(Collectors.toList())
                : Collections.emptyList();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .storeId(order.getStore() != null ? order.getStore().getId() : null)
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .discountAmount(BigDecimal.ZERO)
                .finalTotal(order.getTotalAmount())
                .status(order.getStatus() != null ? order.getStatus().name() : "CREATED")
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now())
                .items(items)
                .build();

        publishOrderCreated(event);
    }

    // 2. PaymentCompleted
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        sendEvent(KafkaTopicConstants.PAYMENT_EVENTS_TOPIC, String.valueOf(event.getOrderId()), event, "PaymentCompletedEvent");
    }

    // 3. PaymentFailed
    public void publishPaymentFailed(PaymentFailedEvent event) {
        sendEvent(KafkaTopicConstants.PAYMENT_EVENTS_TOPIC, String.valueOf(event.getOrderId()), event, "PaymentFailedEvent");
    }

    // 4. OrderConfirmed
    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        sendEvent(KafkaTopicConstants.ORDER_EVENTS_TOPIC, String.valueOf(event.getOrderId()), event, "OrderConfirmedEvent");
    }

    public void publishOrderConfirmed(Order order) {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(order.getId())
                .storeId(order.getStore() != null ? order.getStore().getId() : null)
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .totalAmount(order.getTotalAmount())
                .confirmedAt(LocalDateTime.now())
                .build();
        publishOrderConfirmed(event);
    }

    // 5. OrderReady
    public void publishOrderReady(OrderReadyEvent event) {
        sendEvent(KafkaTopicConstants.ORDER_EVENTS_TOPIC, String.valueOf(event.getOrderId()), event, "OrderReadyEvent");
    }

    public void publishOrderReady(Order order) {
        OrderReadyEvent event = OrderReadyEvent.builder()
                .orderId(order.getId())
                .storeId(order.getStore() != null ? order.getStore().getId() : null)
                .readyAt(LocalDateTime.now())
                .build();
        publishOrderReady(event);
    }

    // 6. RiderAssigned
    public void publishRiderAssigned(RiderAssignedEvent event) {
        sendEvent(KafkaTopicConstants.DELIVERY_EVENTS_TOPIC, String.valueOf(event.getOrderId()), event, "RiderAssignedEvent");
    }

    // 7. OrderDelivered
    public void publishOrderDelivered(OrderDeliveredEvent event) {
        sendEvent(KafkaTopicConstants.DELIVERY_EVENTS_TOPIC, String.valueOf(event.getOrderId()), event, "OrderDeliveredEvent");
    }

    // 8. OrderCancelled
    public void publishOrderCancelled(OrderCancelledEvent event) {
        sendEvent(KafkaTopicConstants.ORDER_EVENTS_TOPIC, String.valueOf(event.getOrderId()), event, "OrderCancelledEvent");
    }

    public void publishOrderCancelled(Order order, String reason, String cancelledBy) {
        List<OrderItemEventDto> items = order.getItems() != null
                ? order.getItems().stream()
                .map(item -> OrderItemEventDto.builder()
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductNameSnapshot())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getSubtotal())
                        .build())
                .collect(Collectors.toList())
                : Collections.emptyList();

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .storeId(order.getStore() != null ? order.getStore().getId() : null)
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .cancellationReason(reason)
                .cancelledBy(cancelledBy)
                .refundAmount(order.getTotalAmount())
                .cancelledAt(LocalDateTime.now())
                .itemsToRestore(items)
                .build();

        publishOrderCancelled(event);
    }

    // 9. RefundCompleted
    public void publishRefundCompleted(RefundCompletedEvent event) {
        sendEvent(KafkaTopicConstants.PAYMENT_EVENTS_TOPIC, String.valueOf(event.getOrderId()), event, "RefundCompletedEvent");
    }

    // Generic asynchronous dispatcher with deterministic key partitioning
    private void sendEvent(String topic, String key, Object event, String eventType) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, payload);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} to topic {} with key {}: {}",
                            eventType, topic, key, ex.getMessage());
                } else {
                    log.info("Published {} to topic {} partition {} offset {} [key={}]",
                            eventType, topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            key);
                }
            });
            log.debug("Initiated dispatch for {} on topic {} with key {}", eventType, topic, key);
        } catch (Exception e) {
            log.error("Serialization or dispatch error while publishing {} to topic {}: {}",
                    eventType, topic, e.getMessage(), e);
        }
    }
}
