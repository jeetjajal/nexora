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
public class OrderConfirmedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long storeId;
    private Long userId;
    private BigDecimal totalAmount;
    private LocalDateTime confirmedAt;
}
