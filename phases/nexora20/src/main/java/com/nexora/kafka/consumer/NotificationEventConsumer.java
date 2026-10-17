package com.nexora.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.kafka.constant.KafkaTopicConstants;
import com.nexora.kafka.event.*;
import com.nexora.notification.dto.NotificationDto;
import com.nexora.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopicConstants.ORDER_EVENTS_TOPIC, groupId = "${spring.kafka.consumer.group-id:nexora-group}")
    public void handleOrderEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (root.has("itemsToRestore") || root.has("cancellationReason")) {
                OrderCancelledEvent event = objectMapper.treeToValue(root, OrderCancelledEvent.class);
                sendNotification("user-" + event.getUserId() + "@nexora.com",
                        "Order Cancelled: #" + event.getOrderId(),
                        "Your order #" + event.getOrderId() + " was cancelled. Reason: " + event.getCancellationReason());
            } else if (root.has("readyAt")) {
                OrderReadyEvent event = objectMapper.treeToValue(root, OrderReadyEvent.class);
                sendNotification("store-" + event.getStoreId() + "@nexora.com",
                        "Order Ready for Pickup: #" + event.getOrderId(),
                        "Order #" + event.getOrderId() + " is packaged and ready for rider pickup.");
            } else if (root.has("confirmedAt")) {
                OrderConfirmedEvent event = objectMapper.treeToValue(root, OrderConfirmedEvent.class);
                sendNotification("user-" + event.getUserId() + "@nexora.com",
                        "Order Confirmed: #" + event.getOrderId(),
                        "The store has accepted your order #" + event.getOrderId() + " and started preparation!");
            } else if (root.has("items")) {
                OrderCreatedEvent event = objectMapper.treeToValue(root, OrderCreatedEvent.class);
                sendNotification("user-" + event.getUserId() + "@nexora.com",
                        "Order Placed: #" + event.getOrderId(),
                        "Thank you! Your order #" + event.getOrderId() + " has been received with total " + event.getFinalTotal());
            }
        } catch (Exception e) {
            log.error("Failed to process order event notification: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopicConstants.PAYMENT_EVENTS_TOPIC, groupId = "${spring.kafka.consumer.group-id:nexora-group}")
    public void handlePaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (root.has("failureReason") || root.has("errorCode")) {
                PaymentFailedEvent event = objectMapper.treeToValue(root, PaymentFailedEvent.class);
                sendNotification("order-" + event.getOrderId() + "@nexora.com",
                        "Payment Failed for Order #" + event.getOrderId(),
                        "Payment of amount " + event.getAmount() + " failed: " + event.getFailureReason());
            } else if (root.has("refundId")) {
                RefundCompletedEvent event = objectMapper.treeToValue(root, RefundCompletedEvent.class);
                sendNotification("order-" + event.getOrderId() + "@nexora.com",
                        "Refund Processed for Order #" + event.getOrderId(),
                        "A refund of " + event.getAmount() + " (Refund ID: " + event.getRefundId() + ") was issued.");
            } else if (root.has("razorpayPaymentId")) {
                PaymentCompletedEvent event = objectMapper.treeToValue(root, PaymentCompletedEvent.class);
                sendNotification("order-" + event.getOrderId() + "@nexora.com",
                        "Payment Successful for Order #" + event.getOrderId(),
                        "We received your payment of " + event.getAmount() + " via " + event.getPaymentMethod());
            }
        } catch (Exception e) {
            log.error("Failed to process payment event notification: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopicConstants.DELIVERY_EVENTS_TOPIC, groupId = "${spring.kafka.consumer.group-id:nexora-group}")
    public void handleDeliveryEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (root.has("deliveryPartnerName")) {
                RiderAssignedEvent event = objectMapper.treeToValue(root, RiderAssignedEvent.class);
                sendNotification("order-" + event.getOrderId() + "@nexora.com",
                        "Rider Assigned for Order #" + event.getOrderId(),
                        "Delivery partner " + event.getDeliveryPartnerName() + " has been assigned to deliver your order.");
            } else if (root.has("deliveredAt")) {
                OrderDeliveredEvent event = objectMapper.treeToValue(root, OrderDeliveredEvent.class);
                sendNotification("user-" + event.getUserId() + "@nexora.com",
                        "Order Delivered: #" + event.getOrderId(),
                        "Your order #" + event.getOrderId() + " has been safely delivered! Enjoy your meal.");
            }
        } catch (Exception e) {
            log.error("Failed to process delivery event notification: {}", message, e);
        }
    }

    private void sendNotification(String to, String subject, String body) {
        NotificationDto dto = new NotificationDto();
        dto.setTo(to);
        dto.setSubject(subject);
        dto.setBody(body);
        notificationService.sendEmail(dto);
        log.info("Dispatched notification email to '{}' - Subject: '{}'", to, subject);
    }
}
