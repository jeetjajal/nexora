package com.nexora.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveredEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long deliveryPartnerId;
    private Long userId;
    private LocalDateTime deliveredAt;
}
