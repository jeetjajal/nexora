package com.nexora.k8s.controller;

import com.nexora.common.ApiResponse;
import com.nexora.k8s.dto.HpaSimulationRequest;
import com.nexora.k8s.dto.HpaSimulationResult;
import com.nexora.k8s.dto.K8sConceptDetail;
import com.nexora.k8s.dto.K8sResourceSpec;
import com.nexora.k8s.service.KubernetesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/k8s")
@RequiredArgsConstructor
public class KubernetesController {

    private final KubernetesService kubernetesService;

    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<List<K8sResourceSpec>>> getResources() {
        return ResponseEntity.ok(ApiResponse.success(
                "Kubernetes deployed resources retrieved successfully",
                kubernetesService.getDeployedResources()
        ));
    }

    @PostMapping("/hpa/simulate")
    public ResponseEntity<ApiResponse<HpaSimulationResult>> simulateHpa(
            @RequestBody HpaSimulationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "HPA scaling simulation computed successfully",
                kubernetesService.simulateHpaScaling(request)
        ));
    }

    @GetMapping("/concepts")
    public ResponseEntity<ApiResponse<List<K8sConceptDetail>>> getConcepts() {
        return ResponseEntity.ok(ApiResponse.success(
                "Kubernetes core concepts retrieved successfully",
                kubernetesService.getCoreConcepts()
        ));
    }
}
