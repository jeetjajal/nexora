package com.nexora.admin.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelayedOrderAlertDto {
    private Long orderId;
    private String orderNumber;
    private String storeName;
    private String customerName;
    private String status;
    private BigDecimal amount;
    private long elapsedSeconds;
    private long targetSlaSeconds;
    private long delayedBySeconds;
    private LocalDateTime createdAt;
}
