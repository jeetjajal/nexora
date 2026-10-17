package com.nexora.aws.controller;

import com.nexora.aws.dto.AwsArchitectureTopology;
import com.nexora.aws.dto.AwsCostEstimate;
import com.nexora.aws.dto.AwsServiceSpec;
import com.nexora.aws.service.AwsArchitectureService;
import com.nexora.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aws")
@RequiredArgsConstructor
public class AwsController {

    private final AwsArchitectureService awsArchitectureService;

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<AwsServiceSpec>>> getServices() {
        return ResponseEntity.ok(ApiResponse.success(
                "AWS managed services catalog retrieved successfully",
                awsArchitectureService.getSelectedServices()
        ));
    }

    @GetMapping("/topology")
    public ResponseEntity<ApiResponse<AwsArchitectureTopology>> getTopology() {
        return ResponseEntity.ok(ApiResponse.success(
                "AWS multi-tier cloud topology retrieved successfully",
                awsArchitectureService.getTopology()
        ));
    }

    @GetMapping("/costs")
    public ResponseEntity<ApiResponse<AwsCostEstimate>> getCostEstimate() {
        return ResponseEntity.ok(ApiResponse.success(
                "AWS production monthly cost estimate retrieved successfully",
                awsArchitectureService.getMonthlyCostEstimate()
        ));
    }
}
