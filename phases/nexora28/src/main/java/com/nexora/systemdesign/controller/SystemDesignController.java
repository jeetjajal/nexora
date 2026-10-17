package com.nexora.systemdesign.controller;

import com.nexora.common.ApiResponse;
import com.nexora.systemdesign.dto.FailureScenarioMatrix;
import com.nexora.systemdesign.dto.InterviewQuestionAnswer;
import com.nexora.systemdesign.dto.ScaleToMillionBlueprint;
import com.nexora.systemdesign.dto.SystemArchitectureDiagram;
import com.nexora.systemdesign.service.SystemDesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system-design")
@RequiredArgsConstructor
public class SystemDesignController {

    private final SystemDesignService systemDesignService;

    @GetMapping("/interview-checklist")
    public ResponseEntity<ApiResponse<List<InterviewQuestionAnswer>>> getInterviewChecklist() {
        return ResponseEntity.ok(ApiResponse.success(
                "Final SDE interview checklist retrieved successfully",
                systemDesignService.getInterviewChecklist()
        ));
    }

    @GetMapping("/architecture")
    public ResponseEntity<ApiResponse<SystemArchitectureDiagram>> getArchitecture() {
        return ResponseEntity.ok(ApiResponse.success(
                "System architecture blueprint retrieved successfully",
                systemDesignService.getArchitectureDiagram()
        ));
    }

    @GetMapping("/scale-to-1m")
    public ResponseEntity<ApiResponse<ScaleToMillionBlueprint>> getScaleBlueprint() {
        return ResponseEntity.ok(ApiResponse.success(
                "Scale to 1 Million Users roadmap retrieved successfully",
                systemDesignService.getScaleToMillionBlueprint()
        ));
    }

    @GetMapping("/failure-matrix")
    public ResponseEntity<ApiResponse<List<FailureScenarioMatrix>>> getFailureMatrix() {
        return ResponseEntity.ok(ApiResponse.success(
                "Failure scenario and automated mitigation matrix retrieved successfully",
                systemDesignService.getFailureScenarios()
        ));
    }
}
