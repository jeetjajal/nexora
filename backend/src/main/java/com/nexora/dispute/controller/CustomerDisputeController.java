package com.nexora.dispute.controller;

import com.nexora.admin.service.AdminControlTowerService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.dispute.dto.CreateDisputeRequest;
import com.nexora.dispute.dto.DisputeResponse;
import com.nexora.dispute.entity.Dispute;
import com.nexora.dispute.repository.DisputeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
public class CustomerDisputeController {

    private final AdminControlTowerService adminControlTowerService;
    private final DisputeRepository disputeRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<DisputeResponse>> createDispute(
            @Valid @RequestBody CreateDisputeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        DisputeResponse response = adminControlTowerService.createDispute(principal, request);
        return ResponseEntity.ok(ApiResponse.success("Dispute logged successfully", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<DisputeResponse>>> getMyDisputes(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Dispute> disputes = disputeRepository.findByCustomerIdOrderByCreatedAtDesc(principal.getId());
        List<DisputeResponse> response = disputes.stream()
                .map(d -> DisputeResponse.builder()
                        .id(d.getId())
                        .orderId(d.getOrder() != null ? d.getOrder().getId() : null)
                        .customerId(d.getCustomer() != null ? d.getCustomer().getId() : null)
                        .customerName(d.getCustomer() != null ? d.getCustomer().getName() : "Customer")
                        .reason(d.getReason())
                        .customerNotes(d.getCustomerNotes())
                        .claimAmount(d.getClaimAmount())
                        .status(d.getStatus())
                        .decisionNotes(d.getDecisionNotes())
                        .refundTransactionId(d.getRefundTransactionId())
                        .createdAt(d.getCreatedAt())
                        .resolvedAt(d.getResolvedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Your disputes retrieved successfully", response));
    }
}
