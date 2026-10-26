package com.nexora.dispute.dto;

import com.nexora.dispute.entity.DisputeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeDecisionRequest {
    @NotNull(message = "Decision status is required (APPROVED_REFUNDED, REJECTED, UNDER_REVIEW)")
    private DisputeStatus decision;

    @NotBlank(message = "Decision explanation notes are required for audit")
    private String decisionNotes;
}
