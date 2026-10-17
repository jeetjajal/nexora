package com.nexora.phase16;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexora.inventory.service.InventoryService;
import com.nexora.kafka.consumer.InventoryEventConsumer;
import com.nexora.kafka.event.OrderCancelledEvent;
import com.nexora.kafka.event.OrderItemEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 16 - InventoryEventConsumer Unit Tests")
class InventoryEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    private ObjectMapper objectMapper;
    private InventoryEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new InventoryEventConsumer(inventoryService, objectMapper);
    }

    @Test
    @DisplayName("Should parse OrderCancelledEvent and restore stock for all items")
    void testRestoreStockOnOrderCancellation() throws Exception {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(501L)
                .storeId(2L)
                .userId(10L)
                .cancellationReason("Item out of stock at store")
                .cancelledBy("STORE_OWNER")
                .refundAmount(new BigDecimal("200.00"))
                .cancelledAt(LocalDateTime.now())
                .itemsToRestore(List.of(
                        OrderItemEventDto.builder().productId(101L).quantity(2).build(),
                        OrderItemEventDto.builder().productId(102L).quantity(3).build()
                ))
                .build();

        String json = objectMapper.writeValueAsString(event);
        consumer.handleOrderEvent(json);

        verify(inventoryService).restoreStock(101L, 2);
        verify(inventoryService).restoreStock(102L, 3);
    }
}
