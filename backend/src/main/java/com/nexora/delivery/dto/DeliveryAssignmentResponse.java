package com.nexora.delivery.dto;

import com.nexora.delivery.entity.AssignmentStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DeliveryAssignmentResponse {
    private Long id;
    private Long orderId;
    private Long partnerId;
    private AssignmentStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
}
