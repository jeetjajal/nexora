package com.nexora.microservice.controller;

import com.nexora.microservice.dto.GatewayRouteConfig;
import com.nexora.microservice.dto.MicroserviceDefinition;
import com.nexora.microservice.dto.SagaSimulationRequest;
import com.nexora.microservice.dto.SagaSimulationResult;
import com.nexora.microservice.service.MicroserviceRegistryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Phase 22: Microservices Architecture Blueprint,
 * API Gateway Route Catalog, and Distributed Saga Orchestration Simulator.
 */
@RestController
@RequestMapping("/api/v1/microservices")
@RequiredArgsConstructor
public class MicroservicesController {

    private final MicroserviceRegistryService registryService;

    /**
     * Retrieve complete microservice decomposition catalog.
     * Contains responsibility, reason for separation, database schema, and failure handling for each service.
     */
    @GetMapping("/catalog")
    public ResponseEntity<List<MicroserviceDefinition>> getCatalog() {
        return ResponseEntity.ok(registryService.getServiceCatalog());
    }

    /**
     * Retrieve API Gateway route mappings and rate limits.
     */
    @GetMapping("/routes")
    public ResponseEntity<List<GatewayRouteConfig>> getRoutes() {
        return ResponseEntity.ok(registryService.getGatewayRoutes());
    }

    /**
     * Retrieve health status of all registered microservices.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> getHealthStatus() {
        return ResponseEntity.ok(registryService.getHealthStatusMap());
    }

    /**
     * Simulate a Distributed Saga transaction across microservices.
     * Demonstrates both happy-path (Order -> Inventory -> Payment -> Notification)
     * and failure rollback with compensating transactions.
     */
    @PostMapping("/saga/simulate")
    public ResponseEntity<SagaSimulationResult> simulateSaga(
            @Valid @RequestBody SagaSimulationRequest request
    ) {
        SagaSimulationResult result = registryService.executeCheckoutSaga(request);
        return ResponseEntity.ok(result);
    }
}
