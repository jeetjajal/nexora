package com.nexora.dispute.dto;

import com.nexora.dispute.entity.DisputeStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String reason;
    private String customerNotes;
    private BigDecimal claimAmount;
    private DisputeStatus status;
    private String decisionNotes;
    private String handledBy;
    private String refundTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
