package com.nexora.phase16;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexora.kafka.consumer.NotificationEventConsumer;
import com.nexora.kafka.event.*;
import com.nexora.notification.dto.NotificationDto;
import com.nexora.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 16 - NotificationEventConsumer Unit Tests")
class NotificationEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private NotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new NotificationEventConsumer(notificationService, objectMapper);
    }

    @Test
    @DisplayName("Should send email when OrderCreatedEvent is consumed")
    void testOrderCreatedNotification() throws Exception {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(801L)
                .storeId(3L)
                .userId(12L)
                .finalTotal(new BigDecimal("150.00"))
                .status("CREATED")
                .items(List.of(OrderItemEventDto.builder().productId(1L).productName("Pizza").quantity(1).build()))
                .build();

        String json = objectMapper.writeValueAsString(event);
        consumer.handleOrderEvent(json);

        ArgumentCaptor<NotificationDto> captor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(notificationService).sendEmail(captor.capture());

        assertThat(captor.getValue().getTo()).isEqualTo("user-12@nexora.com");
        assertThat(captor.getValue().getSubject()).contains("Order Placed: #801");
        assertThat(captor.getValue().getBody()).contains("150");
    }

    @Test
    @DisplayName("Should send email when PaymentCompletedEvent is consumed")
    void testPaymentCompletedNotification() throws Exception {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(901L)
                .orderId(801L)
                .razorpayPaymentId("pay_999")
                .amount(new BigDecimal("150.00"))
                .paymentMethod("CARD")
                .completedAt(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(event);
        consumer.handlePaymentEvent(json);

        ArgumentCaptor<NotificationDto> captor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(notificationService).sendEmail(captor.capture());

        assertThat(captor.getValue().getTo()).isEqualTo("order-801@nexora.com");
        assertThat(captor.getValue().getSubject()).contains("Payment Successful for Order #801");
    }

    @Test
    @DisplayName("Should send email when RiderAssignedEvent is consumed")
    void testRiderAssignedNotification() throws Exception {
        RiderAssignedEvent event = RiderAssignedEvent.builder()
                .orderId(801L)
                .deliveryPartnerId(55L)
                .deliveryPartnerName("Suresh Kumar")
                .deliveryPartnerPhone("9988776655")
                .assignedAt(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(event);
        consumer.handleDeliveryEvent(json);

        ArgumentCaptor<NotificationDto> captor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(notificationService).sendEmail(captor.capture());

        assertThat(captor.getValue().getTo()).isEqualTo("order-801@nexora.com");
        assertThat(captor.getValue().getSubject()).contains("Rider Assigned for Order #801");
        assertThat(captor.getValue().getBody()).contains("Suresh Kumar");
    }
}
