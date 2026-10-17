package com.nexora.kafka.constant;

public final class KafkaTopicConstants {
    private KafkaTopicConstants() {}

    public static final String ORDER_EVENTS_TOPIC = "nexora.order-events";
    public static final String PAYMENT_EVENTS_TOPIC = "nexora.payment-events";
    public static final String DELIVERY_EVENTS_TOPIC = "nexora.delivery-events";
    public static final String DEAD_LETTER_TOPIC = "nexora.dead-letter-topic";
}
