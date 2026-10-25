package com.nexora.microservice.service;

import com.nexora.microservice.dto.GatewayRouteConfig;
import com.nexora.microservice.dto.MicroserviceDefinition;
import com.nexora.microservice.dto.SagaSimulationRequest;
import com.nexora.microservice.dto.SagaSimulationResult;

import java.util.List;
import java.util.Map;

/**
 * Service managing microservice definitions, API Gateway route mappings,
 * and distributed Saga orchestration workflows (Phase 22).
 */
public interface MicroserviceRegistryService {

    /**
     * Retrieve the architectural specifications for all decomposed Nexora microservices.
     */
    List<MicroserviceDefinition> getServiceCatalog();

    /**
     * Retrieve centralized API Gateway routing table.
     */
    List<GatewayRouteConfig> getGatewayRoutes();

    /**
     * Execute or simulate a cross-service Distributed Saga transaction
     * (Order -> Inventory -> Payment -> Notification) with compensating rollback.
     */
    SagaSimulationResult executeCheckoutSaga(SagaSimulationRequest request);

    /**
     * Retrieve real-time health status map of all microservices.
     */
    Map<String, String> getHealthStatusMap();
}
