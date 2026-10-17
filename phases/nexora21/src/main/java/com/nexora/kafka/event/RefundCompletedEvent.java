package com.nexora.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundCompletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String refundId;
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String reason;
    private LocalDateTime refundedAt;
}
