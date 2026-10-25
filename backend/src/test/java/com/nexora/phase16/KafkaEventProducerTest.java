package com.nexora.phase16;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexora.kafka.constant.KafkaTopicConstants;
import com.nexora.kafka.event.*;
import com.nexora.kafka.producer.KafkaEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 16 - KafkaEventProducer Unit Tests")
class KafkaEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private KafkaEventProducer producer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        producer = new KafkaEventProducer(kafkaTemplate, objectMapper);

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);
    }

    @Test
    @DisplayName("Should publish OrderCreatedEvent to nexora.order-events with orderId key")
    void testPublishOrderCreated() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(101L)
                .storeId(5L)
                .userId(1L)
                .subtotal(new BigDecimal("100.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .finalTotal(new BigDecimal("140.00"))
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .items(List.of(OrderItemEventDto.builder().productId(2L).productName("Burger").quantity(2).build()))
                .build();

        producer.publishOrderCreated(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopicConstants.ORDER_EVENTS_TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo("101");
        assertThat(payloadCaptor.getValue()).contains("101").contains("Burger");
    }

    @Test
    @DisplayName("Should publish PaymentCompletedEvent to nexora.payment-events")
    void testPublishPaymentCompleted() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(201L)
                .orderId(101L)
                .razorpayPaymentId("pay_12345")
                .amount(new BigDecimal("140.00"))
                .paymentMethod("UPI")
                .completedAt(LocalDateTime.now())
                .build();

        producer.publishPaymentCompleted(event);

        verify(kafkaTemplate).send(eq(KafkaTopicConstants.PAYMENT_EVENTS_TOPIC), eq("101"), any());
    }

    @Test
    @DisplayName("Should publish PaymentFailedEvent to nexora.payment-events")
    void testPublishPaymentFailed() {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(202L)
                .orderId(102L)
                .errorCode("BAD_REQUEST_PAYMENT_FAILED")
                .failureReason("Insufficient funds")
                .amount(new BigDecimal("250.00"))
                .failedAt(LocalDateTime.now())
                .build();

        producer.publishPaymentFailed(event);

        verify(kafkaTemplate).send(eq(KafkaTopicConstants.PAYMENT_EVENTS_TOPIC), eq("102"), any());
    }

    @Test
    @DisplayName("Should publish OrderConfirmedEvent to nexora.order-events")
    void testPublishOrderConfirmed() {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(103L)
                .storeId(5L)
                .userId(1L)
                .totalAmount(new BigDecimal("300.00"))
                .confirmedAt(LocalDateTime.now())
                .build();

        producer.publishOrderConfirmed(event);

        verify(kafkaTemplate).send(eq(KafkaTopicConstants.ORDER_EVENTS_TOPIC), eq("103"), any());
    }

    @Test
    @DisplayName("Should publish OrderReadyEvent to nexora.order-events")
    void testPublishOrderReady() {
        OrderReadyEvent event = OrderReadyEvent.builder()
                .orderId(104L)
                .storeId(5L)
                .readyAt(LocalDateTime.now())
                .build();

        producer.publishOrderReady(event);

        verify(kafkaTemplate).send(eq(KafkaTopicConstants.ORDER_EVENTS_TOPIC), eq("104"), any());
    }

    @Test
    @DisplayName("Should publish RiderAssignedEvent to nexora.delivery-events")
    void testPublishRiderAssigned() {
        RiderAssignedEvent event = RiderAssignedEvent.builder()
                .orderId(105L)
                .deliveryPartnerId(88L)
                .deliveryPartnerName("John Doe")
                .deliveryPartnerPhone("9876543210")
                .assignedAt(LocalDateTime.now())
                .build();

        producer.publishRiderAssigned(event);

        verify(kafkaTemplate).send(eq(KafkaTopicConstants.DELIVERY_EVENTS_TOPIC), eq("105"), any());
    }

    @Test
    @DisplayName("Should publish OrderDeliveredEvent to nexora.delivery-events")
    void testPublishOrderDelivered() {
        OrderDeliveredEvent event = OrderDeliveredEvent.builder()
                .orderId(106L)
                .deliveryPartnerId(88L)
                .userId(1L)
                .deliveredAt(LocalDateTime.now())
                .build();

        producer.publishOrderDelivered(event);

        verify(kafkaTemplate).send(eq(KafkaTopicConstants.DELIVERY_EVENTS_TOPIC), eq("106"), any());
    }

    @Test
    @DisplayName("Should publish OrderCancelledEvent to nexora.order-events")
    void testPublishOrderCancelled() {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(107L)
                .storeId(5L)
                .userId(1L)
                .cancellationReason("Customer changed mind")
                .cancelledBy("customer")
                .refundAmount(new BigDecimal("180.00"))
                .cancelledAt(LocalDateTime.now())
                .itemsToRestore(List.of(OrderItemEventDto.builder().productId(3L).quantity(1).build()))
                .build();

        producer.publishOrderCancelled(event);

        verify(kafkaTemplate).send(eq(KafkaTopicConstants.ORDER_EVENTS_TOPIC), eq("107"), any());
    }

    @Test
    @DisplayName("Should publish RefundCompletedEvent to nexora.payment-events")
    void testPublishRefundCompleted() {
        RefundCompletedEvent event = RefundCompletedEvent.builder()
                .refundId("rfnd_12345")
                .paymentId(301L)
                .orderId(108L)
                .amount(new BigDecimal("180.00"))
                .reason("Cancelled order refund")
                .refundedAt(LocalDateTime.now())
                .build();

        producer.publishRefundCompleted(event);

        verify(kafkaTemplate).send(eq(KafkaTopicConstants.PAYMENT_EVENTS_TOPIC), eq("108"), any());
    }
}
