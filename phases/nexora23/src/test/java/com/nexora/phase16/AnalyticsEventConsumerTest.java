package com.nexora.phase16;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexora.kafka.consumer.AnalyticsEventConsumer;
import com.nexora.kafka.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Phase 16 - AnalyticsEventConsumer Unit Tests")
class AnalyticsEventConsumerTest {

    private ObjectMapper objectMapper;
    private AnalyticsEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new AnalyticsEventConsumer(objectMapper);
    }

    @Test
    @DisplayName("Should track order count and cumulative GMV")
    void testTrackOrderEvents() throws Exception {
        OrderCreatedEvent order1 = OrderCreatedEvent.builder()
                .orderId(1L)
                .finalTotal(new BigDecimal("250.00"))
                .items(List.of(OrderItemEventDto.builder().productId(1L).quantity(1).build()))
                .build();

        OrderCreatedEvent order2 = OrderCreatedEvent.builder()
                .orderId(2L)
                .finalTotal(new BigDecimal("150.00"))
                .items(List.of(OrderItemEventDto.builder().productId(2L).quantity(2).build()))
                .build();

        consumer.trackOrderEvents(objectMapper.writeValueAsString(order1));
        consumer.trackOrderEvents(objectMapper.writeValueAsString(order2));

        assertThat(consumer.getTotalOrdersCreated().get()).isEqualTo(2);
        assertThat(consumer.getTotalGrossMerchandiseValue().get()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Should track payment outcomes")
    void testTrackPaymentEvents() throws Exception {
        PaymentCompletedEvent paymentSuccess = PaymentCompletedEvent.builder()
                .paymentId(10L)
                .orderId(1L)
                .razorpayPaymentId("pay_123")
                .amount(new BigDecimal("250.00"))
                .build();

        PaymentFailedEvent paymentFailure = PaymentFailedEvent.builder()
                .paymentId(11L)
                .orderId(2L)
                .errorCode("GATEWAY_ERROR")
                .failureReason("Timeout")
                .amount(new BigDecimal("150.00"))
                .build();

        consumer.trackPaymentEvents(objectMapper.writeValueAsString(paymentSuccess));
        consumer.trackPaymentEvents(objectMapper.writeValueAsString(paymentFailure));

        assertThat(consumer.getTotalPaymentsCompleted().get()).isEqualTo(1);
        assertThat(consumer.getTotalPaymentsFailed().get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should track delivered and cancelled orders")
    void testTrackDeliveredAndCancelled() throws Exception {
        OrderDeliveredEvent delivered = OrderDeliveredEvent.builder()
                .orderId(1L)
                .deliveryPartnerId(5L)
                .deliveredAt(LocalDateTime.now())
                .build();

        OrderCancelledEvent cancelled = OrderCancelledEvent.builder()
                .orderId(2L)
                .cancellationReason("Duplicate order")
                .itemsToRestore(List.of())
                .build();

        consumer.trackDeliveryEvents(objectMapper.writeValueAsString(delivered));
        consumer.trackOrderEvents(objectMapper.writeValueAsString(cancelled));

        assertThat(consumer.getTotalOrdersDelivered().get()).isEqualTo(1);
        assertThat(consumer.getTotalOrdersCancelled().get()).isEqualTo(1);
    }
}
