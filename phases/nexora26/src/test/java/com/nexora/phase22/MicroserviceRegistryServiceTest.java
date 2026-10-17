package com.nexora.phase22;

import com.nexora.microservice.dto.GatewayRouteConfig;
import com.nexora.microservice.dto.MicroserviceDefinition;
import com.nexora.microservice.model.MicroserviceId;
import com.nexora.microservice.service.MicroserviceRegistryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 22: MicroserviceRegistryService Unit Tests")
class MicroserviceRegistryServiceTest {

    private MicroserviceRegistryServiceImpl registryService;

    @BeforeEach
    void setUp() {
        registryService = new MicroserviceRegistryServiceImpl();
    }

    @Test
    @DisplayName("Catalog contains all 10 decomposed microservices with complete architectural specs")
    void testServiceCatalogCompleteness() {
        List<MicroserviceDefinition> catalog = registryService.getServiceCatalog();

        assertNotNull(catalog);
        assertEquals(10, catalog.size());

        // Verify each service is present and has non-empty responsibility, reason, schema, and failure strategy
        for (MicroserviceId id : MicroserviceId.values()) {
            MicroserviceDefinition def = catalog.stream()
                    .filter(s -> s.getId() == id)
                    .findFirst()
                    .orElse(null);

            assertNotNull(def, "Missing definition for microservice: " + id);
            assertNotNull(def.getName());
            assertTrue(def.getDefaultPort() >= 8080);
            assertNotNull(def.getResponsibility());
            assertNotNull(def.getReasonForSeparation());
            assertNotNull(def.getDatabaseSchema());
            assertNotNull(def.getCommunicationType());
            assertNotNull(def.getFailureHandlingStrategy());
            assertFalse(def.getPrimaryEndpoints().isEmpty());
        }
    }

    @Test
    @DisplayName("API Gateway routes cover all major domain prefixes")
    void testGatewayRoutes() {
        List<GatewayRouteConfig> routes = registryService.getGatewayRoutes();

        assertNotNull(routes);
        assertFalse(routes.isEmpty());
        assertTrue(routes.stream().anyMatch(r -> r.getRouteId().contains("auth")));
        assertTrue(routes.stream().anyMatch(r -> r.getRouteId().contains("catalog")));
        assertTrue(routes.stream().anyMatch(r -> r.getRouteId().contains("order")));
        assertTrue(routes.stream().anyMatch(r -> r.getRouteId().contains("payment")));
        assertTrue(routes.stream().anyMatch(r -> r.getRouteId().contains("search")));
    }

    @Test
    @DisplayName("Health status map reports UP for all services")
    void testHealthStatusMap() {
        Map<String, String> health = registryService.getHealthStatusMap();

        assertNotNull(health);
        assertEquals(10, health.size());
        for (String status : health.values()) {
            assertTrue(status.startsWith("UP"));
        }
    }
}
