package com.nexora.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.inventory.service.InventoryService;
import com.nexora.kafka.constant.KafkaTopicConstants;
import com.nexora.kafka.event.OrderCancelledEvent;
import com.nexora.kafka.event.OrderItemEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopicConstants.ORDER_EVENTS_TOPIC, groupId = "${spring.kafka.consumer.group-id:nexora-group}")
    public void handleOrderEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            // Check if this is an OrderCancelledEvent
            if (root.has("cancellationReason") || (root.has("status") && "CANCELLED".equalsIgnoreCase(root.get("status").asText()))) {
                OrderCancelledEvent event = objectMapper.treeToValue(root, OrderCancelledEvent.class);
                processOrderCancellation(event);
            }
        } catch (Exception e) {
            log.error("Error processing order event in InventoryEventConsumer: {}", message, e);
            throw new RuntimeException("Inventory event processing failed", e);
        }
    }

    public void processOrderCancellation(OrderCancelledEvent event) {
        log.info("InventoryEventConsumer received OrderCancelledEvent for orderId: {}, reason: {}",
                event.getOrderId(), event.getCancellationReason());

        if (event.getItemsToRestore() != null && !event.getItemsToRestore().isEmpty()) {
            for (OrderItemEventDto item : event.getItemsToRestore()) {
                try {
                    inventoryService.restoreStock(item.getProductId(), item.getQuantity());
                    log.info("Restored {} units to inventory for productId: {} (Order #{})",
                            item.getQuantity(), item.getProductId(), event.getOrderId());
                } catch (Exception ex) {
                    log.error("Failed to restore inventory for productId: {} in Order #{}: {}",
                            item.getProductId(), event.getOrderId(), ex.getMessage(), ex);
                }
            }
        } else {
            log.warn("OrderCancelledEvent for orderId: {} contained no items to restore", event.getOrderId());
        }
    }
}
