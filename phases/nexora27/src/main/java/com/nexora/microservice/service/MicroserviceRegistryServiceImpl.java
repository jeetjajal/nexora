package com.nexora.microservice.service;

import com.nexora.microservice.dto.*;
import com.nexora.microservice.model.MicroserviceId;
import com.nexora.microservice.model.SagaStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Implementation of Nexora Microservices Blueprint and Distributed Saga Orchestration.
 */
@Slf4j
@Service
public class MicroserviceRegistryServiceImpl implements MicroserviceRegistryService {

    private final List<MicroserviceDefinition> catalog = new ArrayList<>();
    private final List<GatewayRouteConfig> routes = new ArrayList<>();

    public MicroserviceRegistryServiceImpl() {
        initServiceCatalog();
        initGatewayRoutes();
    }

    @Override
    public List<MicroserviceDefinition> getServiceCatalog() {
        return Collections.unmodifiableList(catalog);
    }

    @Override
    public List<GatewayRouteConfig> getGatewayRoutes() {
        return Collections.unmodifiableList(routes);
    }

    @Override
    public Map<String, String> getHealthStatusMap() {
        Map<String, String> health = new LinkedHashMap<>();
        for (MicroserviceDefinition s : catalog) {
            health.put(s.getName(), "UP (Port: " + s.getDefaultPort() + ")");
        }
        return health;
    }

    @Override
    public SagaSimulationResult executeCheckoutSaga(SagaSimulationRequest request) {
        String sagaId = "SAGA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        List<SagaStepDetail> steps = new ArrayList<>();

        log.info("Starting Distributed Checkout Saga {} for Customer {} on Product {}",
                sagaId, request.getCustomerId(), request.getProductId());

        // Step 1: Order Service - Create Pending Order
        steps.add(SagaStepDetail.builder()
                .stepNumber(1)
                .microservice("Order Service (:8084)")
                .action("Create Order in CREATED status")
                .status("SUCCESS")
                .compensating(false)
                .details(String.format("Order #%s created with status PENDING_PAYMENT for amount ₹%.2f",
                        sagaId, request.getAmount()))
                .build());

        // Step 2: Inventory Service - Reserve Stock (Pessimistic Lock / Concurrency control)
        steps.add(SagaStepDetail.builder()
                .stepNumber(2)
                .microservice("Inventory Service (:8083)")
                .action("Reserve Inventory Stock")
                .status("SUCCESS")
                .compensating(false)
                .details(String.format("Deducted quantity %d for Product ID %d with row-level lock",
                        request.getQuantity(), request.getProductId()))
                .build());

        // Step 3: Payment Service - Capture Payment via Gateway
        if (request.isSimulatePaymentFailure()) {
            // PAYMENT FAILURE: Trigger Saga Compensating Transactions (Rollback)
            steps.add(SagaStepDetail.builder()
                .stepNumber(3)
                .microservice("Payment Service (:8085)")
                .action("Capture Razorpay Payment")
                .status("FAILED")
                .compensating(false)
                .details("Payment Gateway Error: Bank server timeout or insufficient funds")
                .build());

            // Compensating Step 4: Inventory Service - Release Reserved Stock
            steps.add(SagaStepDetail.builder()
                .stepNumber(4)
                .microservice("Inventory Service (:8083)")
                .action("COMPENSATING: Release & Restore Inventory Stock")
                .status("COMPENSATED")
                .compensating(true)
                .details(String.format("Restored quantity %d for Product ID %d back to stock",
                        request.getQuantity(), request.getProductId()))
                .build());

            // Compensating Step 5: Order Service - Cancel Order
            steps.add(SagaStepDetail.builder()
                .stepNumber(5)
                .microservice("Order Service (:8084)")
                .action("COMPENSATING: Mark Order CANCELLED")
                .status("COMPENSATED")
                .compensating(true)
                .details(String.format("Order #%s marked CANCELLED due to payment failure", sagaId))
                .build());

            return SagaSimulationResult.builder()
                    .sagaId(sagaId)
                    .status(SagaStatus.ROLLED_BACK_FAILED)
                    .overallSuccess(false)
                    .executionSteps(steps)
                    .summary("Saga Failed at Payment Step. Compensating transactions executed to release reserved stock and cancel order.")
                    .compensationSummary("COMPENSATING SUCCESS: Zero stock leaked. Inventory restored and order cancelled cleanly.")
                    .build();
        } else {
            // SUCCESS FLOW
            steps.add(SagaStepDetail.builder()
                .stepNumber(3)
                .microservice("Payment Service (:8085)")
                .action("Capture Razorpay Payment")
                .status("SUCCESS")
                .compensating(false)
                .details(String.format("Razorpay Payment verified for ₹%.2f. Transaction ID tx_%s",
                        request.getAmount(), UUID.randomUUID().toString().substring(0, 8)))
                .build());

            // Step 4: Order Service - Mark Confirmed
            steps.add(SagaStepDetail.builder()
                .stepNumber(4)
                .microservice("Order Service (:8084)")
                .action("Update Order Status to CONFIRMED")
                .status("SUCCESS")
                .compensating(false)
                .details("Order transitioned to CONFIRMED. Emitted OrderConfirmedEvent to Kafka.")
                .build());

            // Step 5: Notification Service - Dispatch Async Email/SMS
            steps.add(SagaStepDetail.builder()
                .stepNumber(5)
                .microservice("Notification Service (:8087)")
                .action("Dispatch Order Confirmation Email & SMS")
                .status("SUCCESS")
                .compensating(false)
                .details("Kafka event consumed asynchronously. Customer SMS & email delivered.")
                .build());

            return SagaSimulationResult.builder()
                    .sagaId(sagaId)
                    .status(SagaStatus.COMPLETED_SUCCESSFULLY)
                    .overallSuccess(true)
                    .executionSteps(steps)
                    .summary("Distributed Saga completed successfully across Order, Inventory, Payment, and Notification microservices.")
                    .compensationSummary("No compensation needed.")
                    .build();
        }
    }

    private void initServiceCatalog() {
        catalog.add(MicroserviceDefinition.builder()
                .id(MicroserviceId.API_GATEWAY)
                .name("API Gateway")
                .defaultPort(8080)
                .responsibility("Unified public entry point, reverse proxy routing, global rate limiting, and centralized JWT validation.")
                .reasonForSeparation("Shields internal microservice network topology from external clients; enforces uniform security policies.")
                .primaryEndpoints(List.of("/api/v1/**", "/ws-nexora/**"))
                .databaseSchema("Stateless (Redis token bucket for rate limiting)")
                .communicationType("Spring Cloud Gateway / Reactive Netty HTTP")
                .failureHandlingStrategy("Resilience4j Circuit Breaker with HTTP 503 / 429 fallback")
                .status("ONLINE")
                .build());

        catalog.add(MicroserviceDefinition.builder()
                .id(MicroserviceId.AUTH_USER_SERVICE)
                .name("Auth & User Service")
                .defaultPort(8081)
                .responsibility("User registration, authentication, BCrypt password hashing, JWT token issuance, and profile management.")
                .reasonForSeparation("Centralizes identity governance and RBAC authorization without distributing password secrets across services.")
                .primaryEndpoints(List.of("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/users/profile"))
                .databaseSchema("nexora_auth_db (users, roles, addresses, user_roles)")
                .communicationType("Synchronous REST (OpenFeign) + Stateless JWT")
                .failureHandlingStrategy("Public key caching; database read replica failover")
                .status("ONLINE")
                .build());

        catalog.add(MicroserviceDefinition.builder()
                .id(MicroserviceId.PRODUCT_CATALOG_SERVICE)
                .name("Product Catalog Service")
                .defaultPort(8082)
                .responsibility("Store merchant catalog, category taxonomies, menu items, prices, descriptions, and food images.")
                .reasonForSeparation("Extremely read-heavy (95%+ reads). Needs aggressive caching and independent horizontal scaling.")
                .primaryEndpoints(List.of("/api/v1/stores/**", "/api/v1/products/**", "/api/v1/categories/**"))
                .databaseSchema("nexora_catalog_db (stores, products, categories, store_categories)")
                .communicationType("REST + Redis Cache + Kafka Producer for catalog change events")
                .failureHandlingStrategy("Stale cache fallback if database is under maintenance")
                .status("ONLINE")
                .build());

        catalog.add(MicroserviceDefinition.builder()
                .id(MicroserviceId.INVENTORY_SERVICE)
                .name("Inventory Service")
                .defaultPort(8083)
                .responsibility("Real-time stock reservation, atomic stock reduction, flash sale pessimistic locking, and restock tracking.")
                .reasonForSeparation("High-contention write operations. Isolates database locking overhead from product catalog queries.")
                .primaryEndpoints(List.of("/api/v1/products/{id}/inventory", "/api/v1/concurrency/simulate"))
                .databaseSchema("nexora_inventory_db (inventory, version, low_stock_threshold)")
                .communicationType("REST (Pessimistic/Optimistic Locking) + Kafka Consumer for restock rollback")
                .failureHandlingStrategy("Dead-letter queue (DLQ) for failed rollback events; Redis distributed lock lease timeout")
                .status("ONLINE")
                .build());

        catalog.add(MicroserviceDefinition.builder()
                .id(MicroserviceId.ORDER_SERVICE)
                .name("Order Service")
                .defaultPort(8084)
                .responsibility("Cart checkout, order placement, order state machine lifecycle (CREATED -> CONFIRMED -> DELIVERED).")
                .reasonForSeparation("Core business domain engine. Orchestrates cross-service distributed Saga transactions.")
                .primaryEndpoints(List.of("/api/v1/cart/**", "/api/v1/orders/**"))
                .databaseSchema("nexora_order_db (orders, order_items, order_status_history)")
                .communicationType("Kafka Producer (OrderCreatedEvent, OrderCancelledEvent) + REST")
                .failureHandlingStrategy("Distributed Saga Orchestrator with compensating transaction rollbacks on failure")
                .status("ONLINE")
                .build());

        catalog.add(MicroserviceDefinition.builder()
                .id(MicroserviceId.PAYMENT_SERVICE)
                .name("Payment Service")
                .defaultPort(8085)
                .responsibility("Razorpay payment gateway integration, SHA-256 signature verification, webhook processing, automated refunds.")
                .reasonForSeparation("Strict PCI-DSS financial security compliance. Financial audit trails isolated from application logic.")
                .primaryEndpoints(List.of("/api/v1/payments/checkout", "/api/v1/payments/verify", "/api/v1/payments/webhook"))
                .databaseSchema("nexora_payment_db (payments, payment_transactions, idempotency_keys)")
                .communicationType("HTTPS Razorpay Webhooks + Kafka Producer (PaymentCapturedEvent, PaymentFailedEvent)")
                .failureHandlingStrategy("Idempotent replay handling, exponential backoff retries, and dead-letter queues")
                .status("ONLINE")
                .build());

        catalog.add(MicroserviceDefinition.builder()
                .id(MicroserviceId.DELIVERY_SERVICE)
                .name("Delivery Tracking Service")
                .defaultPort(8086)
                .responsibility("Delivery partner location tracking, GPS coordinate ingestion, WebSocket live dashboard streaming.")
                .reasonForSeparation("High-frequency real-time bidirectional WebSocket connections that would exhaust standard HTTP worker threads.")
                .primaryEndpoints(List.of("/ws-nexora/**", "/api/v1/delivery/**", "/tracking.html"))
                .databaseSchema("nexora_delivery_db (delivery_assignments, rider_locations)")
                .communicationType("Spring WebSocket (STOMP) + Kafka Consumer for OrderReadyForPickupEvent")
                .failureHandlingStrategy("Fallback to last known GPS coordinates if delivery partner loses network")
                .status("ONLINE")
                .build());

        catalog.add(MicroserviceDefinition.builder()
                .id(MicroserviceId.NOTIFICATION_SERVICE)
                .name("Notification Service")
                .defaultPort(8087)
                .responsibility("Asynchronous order confirmation emails, SMS alerts, and mobile push notifications.")
                .reasonForSeparation("Slow and variable latency of third-party SMS/email providers must never slow down customer checkout.")
                .primaryEndpoints(List.of("/api/v1/notifications/history"))
                .databaseSchema("Stateless / Redis delivery status log")
                .communicationType("Kafka Consumer (Subscribes to order-events and payment-events)")
                .failureHandlingStrategy("Automatic retry with exponential backoff and dead letter queue for invalid recipient contacts")
                .status("ONLINE")
                .build());

        catalog.add(MicroserviceDefinition.builder()
                .id(MicroserviceId.SEARCH_SERVICE)
                .name("Search Service")
                .defaultPort(8088)
                .responsibility("High-speed full-text product search, typo-tolerant fuzzy matching, autocomplete suggestions, category filtering.")
                .reasonForSeparation("Elasticsearch requires substantial JVM heap and RAM. Decoupling protects transactional database memory.")
                .primaryEndpoints(List.of("/api/v1/search/products", "/api/v1/search/stores", "/api/v1/search/autocomplete"))
                .databaseSchema("Elasticsearch Clusters (products_index, stores_index)")
                .communicationType("REST + Kafka Consumer for real-time Change Data Capture (CDC)")
                .failureHandlingStrategy("Graceful degradation to database LIKE query fallback if Elasticsearch cluster is offline")
                .status("ONLINE")
                .build());

        catalog.add(MicroserviceDefinition.builder()
                .id(MicroserviceId.AI_RECOMMENDATION_SERVICE)
                .name("AI & Recommendation Service")
                .defaultPort(8089)
                .responsibility("Personalized 'Recommended For You' scoring, Frequently Bought Together basket analysis, Google Gemini AI Chatbot.")
                .reasonForSeparation("External AI LLM network latency and complex recommendation scoring matrix isolated from checkout.")
                .primaryEndpoints(List.of("/api/v1/ai/chat", "/api/v1/recommendations/for-you", "/api/v1/recommendations/trending"))
                .databaseSchema("Redis recommendation caches (nexora:rec:*) + Analytics read replicas")
                .communicationType("REST + Gemini API Client + Fallback Offline Stub Assistant")
                .failureHandlingStrategy("Automatic fallback to offline rule-based assistant when Gemini API key or quota is unavailable")
                .status("ONLINE")
                .build());
    }

    private void initGatewayRoutes() {
        routes.add(GatewayRouteConfig.builder()
                .routeId("auth-service-route")
                .pathPattern("/api/v1/auth/**")
                .targetServiceUri("http://localhost:8081")
                .appliedFilters(List.of("CorsFilter", "RateLimiterFilter"))
                .rateLimitRps(50)
                .build());

        routes.add(GatewayRouteConfig.builder()
                .routeId("catalog-service-route")
                .pathPattern("/api/v1/products/**, /api/v1/stores/**")
                .targetServiceUri("http://localhost:8082")
                .appliedFilters(List.of("CorsFilter", "CacheControlFilter"))
                .rateLimitRps(200)
                .build());

        routes.add(GatewayRouteConfig.builder()
                .routeId("inventory-service-route")
                .pathPattern("/api/v1/concurrency/**")
                .targetServiceUri("http://localhost:8083")
                .appliedFilters(List.of("CorsFilter", "JwtAuthFilter"))
                .rateLimitRps(100)
                .build());

        routes.add(GatewayRouteConfig.builder()
                .routeId("order-service-route")
                .pathPattern("/api/v1/orders/**, /api/v1/cart/**")
                .targetServiceUri("http://localhost:8084")
                .appliedFilters(List.of("CorsFilter", "JwtAuthFilter", "IdempotencyFilter"))
                .rateLimitRps(100)
                .build());

        routes.add(GatewayRouteConfig.builder()
                .routeId("payment-service-route")
                .pathPattern("/api/v1/payments/**")
                .targetServiceUri("http://localhost:8085")
                .appliedFilters(List.of("CorsFilter", "WebhookSignatureValidatorFilter"))
                .rateLimitRps(50)
                .build());

        routes.add(GatewayRouteConfig.builder()
                .routeId("search-service-route")
                .pathPattern("/api/v1/search/**")
                .targetServiceUri("http://localhost:8088")
                .appliedFilters(List.of("CorsFilter", "ResponseCacheFilter"))
                .rateLimitRps(300)
                .build());

        routes.add(GatewayRouteConfig.builder()
                .routeId("ai-recommendations-route")
                .pathPattern("/api/v1/ai/**, /api/v1/recommendations/**")
                .targetServiceUri("http://localhost:8089")
                .appliedFilters(List.of("CorsFilter", "JwtAuthFilter"))
                .rateLimitRps(30)
                .build());
    }
}
