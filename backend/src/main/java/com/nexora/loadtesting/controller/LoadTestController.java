package com.nexora.loadtesting.controller;

import com.nexora.common.ApiResponse;
import com.nexora.loadtesting.dto.BeforeAfterOptimizationReport;
import com.nexora.loadtesting.dto.LoadTestResult;
import com.nexora.loadtesting.dto.LoadTestScenario;
import com.nexora.loadtesting.service.LoadTestingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loadtest")
@RequiredArgsConstructor
public class LoadTestController {

    private final LoadTestingService loadTestingService;

    @GetMapping("/scenarios")
    public ResponseEntity<ApiResponse<List<LoadTestScenario>>> getScenarios() {
        return ResponseEntity.ok(ApiResponse.success(
                "Load test scenarios retrieved successfully",
                loadTestingService.getAllScenarios()
        ));
    }

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<LoadTestResult>> runScenario(
            @RequestParam(defaultValue = "product_search") String scenarioId,
            @RequestParam(defaultValue = "50") int virtualUsers,
            @RequestParam(defaultValue = "10") int durationSeconds) {
        return ResponseEntity.ok(ApiResponse.success(
                "Load test execution completed successfully",
                loadTestingService.runScenarioLoadTest(scenarioId, virtualUsers, durationSeconds)
        ));
    }

    @GetMapping("/benchmark/before-after")
    public ResponseEntity<ApiResponse<List<BeforeAfterOptimizationReport>>> getBenchmarkReport() {
        return ResponseEntity.ok(ApiResponse.success(
                "Before vs After optimization benchmark report retrieved successfully",
                loadTestingService.getOptimizationReports()
        ));
    }
}
