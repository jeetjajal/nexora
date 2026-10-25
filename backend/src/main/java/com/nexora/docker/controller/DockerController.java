package com.nexora.docker.controller;

import com.nexora.common.ApiResponse;
import com.nexora.docker.dto.DockerConceptExplanation;
import com.nexora.docker.dto.DockerContainerSpec;
import com.nexora.docker.dto.DockerTopologyResponse;
import com.nexora.docker.service.DockerTopologyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/docker")
@RequiredArgsConstructor
public class DockerController {

    private final DockerTopologyService dockerTopologyService;

    @GetMapping("/containers")
    public ResponseEntity<ApiResponse<List<DockerContainerSpec>>> getContainers() {
        return ResponseEntity.ok(ApiResponse.success(
                "Container specifications retrieved successfully",
                dockerTopologyService.getAllContainers()
        ));
    }

    @GetMapping("/topology")
    public ResponseEntity<ApiResponse<DockerTopologyResponse>> getTopology() {
        return ResponseEntity.ok(ApiResponse.success(
                "Docker compose topology retrieved successfully",
                dockerTopologyService.getTopology()
        ));
    }

    @GetMapping("/concepts")
    public ResponseEntity<ApiResponse<List<DockerConceptExplanation>>> getConcepts() {
        return ResponseEntity.ok(ApiResponse.success(
                "Docker containerization concepts retrieved successfully",
                dockerTopologyService.getConceptExplanations()
        ));
    }
}
