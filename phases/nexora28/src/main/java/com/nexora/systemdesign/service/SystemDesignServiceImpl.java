package com.nexora.systemdesign.service;

import com.nexora.systemdesign.dto.FailureScenarioMatrix;
import com.nexora.systemdesign.dto.InterviewQuestionAnswer;
import com.nexora.systemdesign.dto.ScaleToMillionBlueprint;
import com.nexora.systemdesign.dto.SystemArchitectureDiagram;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SystemDesignServiceImpl implements SystemDesignService {

    private final List<InterviewQuestionAnswer> checklist = Arrays.asList(
            InterviewQuestionAnswer.builder()
                    .questionId("q1_spring_boot_mvc")
                    .question("Why Spring Boot is used and how Controller -> Service -> Repository works?")
                    .category("Architecture Foundation")
                    .shortAnswer("Enforces clean separation of concerns: Controller handles HTTP/validation, Service implements business rules & transactions, Repository interacts with DB.")
                    .inDepthExplanation("Spring Boot eliminates boilerplate XML configuration, provides embedded Tomcat, and leverages IoC/DI. Controller maps requests and validates DTOs via JSR-380 (@Valid). Service coordinates domain logic inside @Transactional boundaries (ACID). Repository uses Spring Data JPA to generate optimized queries and interfaces with Hibernate.")
                    .nexoraImplementationProof("AuthController, OrderServiceImpl, OrderRepository across com.nexora package.")
                    .interviewKeywords(Arrays.asList("Separation of Concerns", "Inversion of Control", "Declarative Transactions", "Layered Architecture"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q2_mysql_isolation")
                    .question("Explain MySQL relationships, indexes, transactions and isolation.")
                    .category("Database & Persistence")
                    .shortAnswer("MySQL 8 InnoDB engine enforces ACID guarantees, B+ Tree indexes for O(log N) lookups, foreign keys for referential integrity, and REPEATABLE READ isolation.")
                    .inDepthExplanation("Composite and single-column indexes on user_id, order_id, store_id prevent full table scans. Transactions guarantee Atomicity, Consistency, Isolation, and Durability. REPEATABLE READ with MVCC prevents dirty reads and non-repeatable reads; row-level next-key locks prevent phantom rows.")
                    .nexoraImplementationProof("Composite indexes in flyway migrations / entities: idx_orders_user_id, idx_order_items_order_id.")
                    .interviewKeywords(Arrays.asList("ACID", "B+ Tree", "REPEATABLE READ", "MVCC", "Next-Key Locking"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q3_jwt_auth")
                    .question("Explain JWT authentication and role-based authorization.")
                    .category("Security & Identity")
                    .shortAnswer("Stateless tokens containing cryptographic HMAC-SHA256 signatures; role-based access verified at method level via @PreAuthorize.")
                    .inDepthExplanation("Client authenticates once; server issues signed JWT containing claims (subject, roles, expiry). JwtAuthenticationFilter intercepts each HTTP request, verifies HMAC signature with 256-bit secret, and sets SecurityContextHolder. No session state on server, enabling effortless horizontal scaling across pods.")
                    .nexoraImplementationProof("JwtService, JwtAuthenticationFilter, SecurityConfig with @EnableMethodSecurity.")
                    .interviewKeywords(Arrays.asList("Stateless JWT", "HMAC-SHA256", "SecurityContextHolder", "RBAC", "Bearer Token"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q4_tamper_proof_totals")
                    .question("Explain how the cart and order totals are protected from frontend tampering.")
                    .category("Commerce Security")
                    .shortAnswer("Never trust frontend prices; backend fetches authoritative prices directly from MySQL and recalculates subtotals, taxes, and fees server-side.")
                    .inDepthExplanation("The client only passes product IDs and desired quantities. OrderServiceImpl loads fresh Product entities from database within a database transaction, verifies price, applies validated coupons, computes delivery fee, and derives total amount. Tampered payloads have zero effect.")
                    .nexoraImplementationProof("OrderServiceImpl.checkout() calculates subtotal = item.getQuantity() * product.getPrice().")
                    .interviewKeywords(Arrays.asList("Server-Side Price Authority", "Zero-Trust Client", "Transaction Isolation"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q5_payment_verification")
                    .question("Explain payment verification, webhooks and idempotency.")
                    .category("Payments & Consistency")
                    .shortAnswer("Verify cryptographic signature of payment gateway webhooks; enforce idempotency keys to ensure payments are never processed twice.")
                    .inDepthExplanation("Razorpay signs webhook payloads with a secret using HMAC-SHA256. RazorpaySignatureVerifier recalculates hash and compares using constant-time check. Idempotency table records transaction ID; repeated webhooks or network retries return cached response without double charge.")
                    .nexoraImplementationProof("RazorpaySignatureVerifier, PaymentWebhookController, IdempotencyRecordRepository.")
                    .interviewKeywords(Arrays.asList("Webhook Verification", "Constant-Time Compare", "Idempotency Key", "HMAC-SHA256"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q6_duplicate_payments")
                    .question("Explain how duplicate payments are prevented.")
                    .category("Payments Reliability")
                    .shortAnswer("Unique database constraints on razorpay_payment_id + idempotency check + payment status state machine guards.")
                    .inDepthExplanation("Payment table has a UNIQUE constraint on razorpay_payment_id. If a user double-clicks pay or network retries webhook, database rejects duplicate insertion with DataIntegrityViolationException. Order state transition is strictly CREATED -> PAYMENT_PENDING -> PAID.")
                    .nexoraImplementationProof("payments table constraint unique_razorpay_payment_id and OrderStatus transition validator.")
                    .interviewKeywords(Arrays.asList("Unique Database Constraints", "State Machine Guards", "Optimistic Idempotency"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q7_inventory_concurrency")
                    .question("Explain how inventory overselling is prevented under concurrency.")
                    .category("Concurrency & High-Throughput")
                    .shortAnswer("Pessimistic row-level locking (SELECT ... FOR UPDATE) and Redis token bucket rate limiting guarantee zero overselling.")
                    .inDepthExplanation("When 100 users buy stock=1 simultaneously, InventoryRepository executes SELECT ... FOR UPDATE, acquiring an exclusive lock on that specific row. Other transactions wait in queue. The first transaction decrements stock to 0 and commits; subsequent transactions read stock=0 and fail gracefully with OutOfStockException.")
                    .nexoraImplementationProof("InventoryRepository.findByProductIdWithPessimisticWriteLock, FlashSaleConcurrencyStressIntegrationTest.")
                    .interviewKeywords(Arrays.asList("Pessimistic Locking", "SELECT FOR UPDATE", "Lock Contention", "Atomic Decrement"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q8_redis_caching")
                    .question("Explain Redis cache hit/miss and invalidation.")
                    .category("Caching & Performance")
                    .shortAnswer("Cache-Aside pattern with TTL; MySQL remains source of truth, cache invalidated on product/store updates.")
                    .inDepthExplanation("On read, service checks Redis key. Hit -> returns in <2ms. Miss -> queries MySQL, stores in Redis with TTL (e.g. 10m), and returns. On write (update price/stock), service invalidates Redis cache key immediately (Cache Invalidation) or uses write-through.")
                    .nexoraImplementationProof("RedisCacheService, RedisCartCacheService, ProductServiceImpl with cache eviction.")
                    .interviewKeywords(Arrays.asList("Cache-Aside Pattern", "TTL Expiration", "Cache Invalidation", "Sub-millisecond Latency"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q9_kafka_events")
                    .question("Explain Kafka producers, consumers, events and failure handling.")
                    .category("Distributed Event Streaming")
                    .shortAnswer("Asynchronous decoupled event stream; OrderCreated events dispatched to Kafka, consumed by Inventory, Delivery, Notification.")
                    .inDepthExplanation("Kafka producer publishes events to partitioned topics with acks=all and retries=3. Consumer groups process messages independently with committed offset tracking. Failed events are routed to Dead Letter Topic (DLT) with exponential backoff retry to prevent head-of-line blocking.")
                    .nexoraImplementationProof("KafkaEventProducer, InventoryEventConsumer, NotificationEventConsumer, DLT error handler.")
                    .interviewKeywords(Arrays.asList("Event-Driven Architecture", "Consumer Groups", "Partitioning", "Dead Letter Queue"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q10_rest_vs_websocket")
                    .question("Explain REST vs WebSocket.")
                    .category("Networking & Protocols")
                    .shortAnswer("REST is stateless half-duplex HTTP request-response. WebSocket is stateful full-duplex bi-directional TCP connection over a single handshake.")
                    .inDepthExplanation("REST is ideal for CRUD, search, and checkout where operations are discrete. WebSocket (over STOMP) is used for real-time rider tracking and live order status, eliminating continuous HTTP polling overhead (saving 95% bandwidth and server CPU).")
                    .nexoraImplementationProof("WebSocketConfig, OrderTrackingWebSocketService, /ws-nexora endpoint.")
                    .interviewKeywords(Arrays.asList("Full-Duplex TCP", "STOMP Over WebSocket", "Polling Elimination", "Low Overhead"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q11_search_engine")
                    .question("Explain why a search engine is used in addition to MySQL.")
                    .category("Search & Indexing")
                    .shortAnswer("MySQL B-Trees fail on leading wildcards LIKE '%query%'; Elasticsearch inverted indexes enable sub-second fuzzy, typo-tolerant full-text search.")
                    .inDepthExplanation("MySQL is optimized for row-based ACID transactions. Elasticsearch indexes text into tokenized inverted indices, supporting BM25 scoring, fuzzy matching, phonetic search, and faceted aggregations across millions of items without database table locks.")
                    .nexoraImplementationProof("ProductSearchRepository, SearchSyncServiceImpl, ProductSearchServiceImpl.")
                    .interviewKeywords(Arrays.asList("Inverted Index", "BM25 Ranking", "Fuzzy Search", "Faceted Search"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q12_ai_tool_calling")
                    .question("Explain how AI tool calling safely accesses backend APIs.")
                    .category("AI Integration & Security")
                    .shortAnswer("LLM never accesses MySQL directly; it invokes strictly typed Java tools passing through authentication and validation.")
                    .inDepthExplanation("Gemini or Claude acts as a reasoning engine. Customer requests prompt the model to call declared backend functions (e.g. getOrderStatus, searchProducts). The backend executes the tool with user's JWT claims, ensuring tenants cannot view other users' orders.")
                    .nexoraImplementationProof("AiChatServiceImpl, OrderAiTools, StoreAndProductAiTools with authorization checks.")
                    .interviewKeywords(Arrays.asList("Tool/Function Calling", "Zero Direct DB Access", "Context Injection", "Guardrails"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q13_monolith_vs_microservices")
                    .question("Explain modular monolith vs microservices and why you delayed the split.")
                    .category("System Evolution")
                    .shortAnswer("Modular monolith minimizes distributed systems overhead early on; microservices introduced only when independent scaling/deployments are justified.")
                    .inDepthExplanation("Early microservices bring distributed transactions, network latency, saga rollback complexity, and deployment overhead. A clean modular monolith with well-defined module boundaries (order, auth, catalog) enables fast iteration. We split into 10 microservices when scaling requirements demanded database-per-service isolation.")
                    .nexoraImplementationProof("MicroserviceRegistryServiceImpl, GatewayRouteConfig, SagaOrchestratorService.")
                    .interviewKeywords(Arrays.asList("Modular Monolith", "Service Decomposition", "Distributed Saga", "Database-Per-Service"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q14_docker_aws_k8s")
                    .question("Explain Docker, AWS and Kubernetes at a high level.")
                    .category("DevOps & Cloud")
                    .shortAnswer("Docker packages application & dependencies into immutable containers; Kubernetes orchestrates pod scaling & health; AWS provides managed infrastructure.")
                    .inDepthExplanation("Docker eliminates environment drift via multi-stage builds. Kubernetes manages container lifecycle, rolling updates, and HPA autoscaling (1 to 5 pods). AWS provides the underlying cloud substrate: Multi-AZ VPC, ALB for TLS ingress, ECS Fargate, RDS MySQL, and ElastiCache Redis.")
                    .nexoraImplementationProof("Dockerfile, docker-compose.yml, main.tf, k8s/deployment-order-service.yaml.")
                    .interviewKeywords(Arrays.asList("Containerization", "Pod Orchestration", "HPA", "Multi-AZ Cloud", "Infrastructure-as-Code"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q15_metrics_latency")
                    .question("Explain P95/P99 latency, throughput, error rate and Kafka lag.")
                    .category("Observability & SRE")
                    .shortAnswer("P95/P99 latency measures tail latency experienced by worst 5% and 1% of users; throughput is RPS; Kafka lag measures consumer backlog.")
                    .inDepthExplanation("Averages hide performance degradations. P95 and P99 reveal edge cases like lock contention, GC pauses, or slow DB queries. Kafka consumer lag measures unread messages in topic partition; high lag warns that downstream processors (e.g. notification) are falling behind.")
                    .nexoraImplementationProof("MetricsCollectorServiceImpl, PrometheusAlertRule, /actuator/metrics.")
                    .interviewKeywords(Arrays.asList("Tail Latency (P99)", "Percentile Histograms", "Throughput (RPS)", "Consumer Lag"))
                    .build(),

            InterviewQuestionAnswer.builder()
                    .questionId("q16_scale_to_one_million")
                    .question("Explain how Nexora could scale to 1 million users.")
                    .category("High-Scale Architecture")
                    .shortAnswer("Multi-tier horizontal scaling: CloudFront CDN -> ALB -> K8s Pods -> Redis Cluster -> MySQL Primary/Read-Replicas with sharding.")
                    .inDepthExplanation("1. Static Assets: Offloaded to CloudFront CDN (99% cache hit). 2. Reads: Cached in Redis Cluster (88%+ hit rate); remaining reads load-balanced across 3 MySQL Read-Replicas. 3. Writes: Pessimistic locking + database sharding by user_id/store_id. 4. Compute: K8s HPA scales pods from 5 to 50 across Multi-AZ. 5. Async: Kafka buffers checkout events to protect downstream services.")
                    .nexoraImplementationProof("ScaleToMillionBlueprint, HPA simulation, Redis caching, Kafka event streaming.")
                    .interviewKeywords(Arrays.asList("Read Replicas", "Database Sharding", "Multi-Tier Caching", "Kafka Backpressure Buffer"))
                    .build()
    );

    private final List<FailureScenarioMatrix> failureScenarios = Arrays.asList(
            FailureScenarioMatrix.builder()
                    .failureEvent("MySQL Primary Database Crashes")
                    .impact("Write operations halted; orders and checkouts unable to commit.")
                    .automatedMitigation("AWS RDS Multi-AZ automatically detects health failure and promotes standby replica in AZ-b to Primary within 60-120s with zero data loss.")
                    .recoveryProtocol("DNS failover automatically redirects jdbc connection string; HikariCP reconnects pool; read operations continue against surviving replicas.")
                    .dataConsistencyGuarantee("Strict ACID consistency preserved; synchronous storage replication prevents lost writes.")
                    .build(),

            FailureScenarioMatrix.builder()
                    .failureEvent("Redis Cluster Node Outage")
                    .impact("Cache miss spike; temporary increased read load on MySQL.")
                    .automatedMitigation("Redis Sentinel / ElastiCache auto-failover promotes replica node in <15 seconds; Nexora CacheService falls back gracefully to MySQL.")
                    .recoveryProtocol("Circuit breaker prevents database exhaustion; Redis AOF/RDB restores key state upon node restart.")
                    .dataConsistencyGuarantee("Cache-aside guarantees MySQL remains immutable single source of truth.")
                    .build(),

            FailureScenarioMatrix.builder()
                    .failureEvent("Apache Kafka Broker Unavailable")
                    .impact("Asynchronous order notification and delivery event publishing fails.")
                    .automatedMitigation("Kafka Producer buffers messages in local memory; retries with exponential backoff (retries=3, acks=all).")
                    .recoveryProtocol("If broker remains down, producer writes events to Outbox Table in MySQL as part of primary transaction, drained upon Kafka recovery.")
                    .dataConsistencyGuarantee("Transactional Outbox Pattern guarantees at-least-once message delivery.")
                    .build(),

            FailureScenarioMatrix.builder()
                    .failureEvent("Payment Gateway Succeeds but Order Creation Fails")
                    .impact("Customer charged, but order not placed in database.")
                    .automatedMitigation("Distributed Saga Orchestrator triggers compensating transaction: calls Razorpay Refund API automatically and logs incident.")
                    .recoveryProtocol("Webhook reconciliation cron reconciles orphan payment records every 5 minutes and issues automated refund or recovers order.")
                    .dataConsistencyGuarantee("Compensating Saga rollback ensures customer is never left charged without an active order.")
                    .build(),

            FailureScenarioMatrix.builder()
                    .failureEvent("Flash Sale Traffic Spike (10,000 Concurrent Users)")
                    .impact("Server CPU surge and thread starvation.")
                    .automatedMitigation("Kubernetes HPA scales pods rapidly (min 1 -> max 5/50 pods); Redis token bucket rate limits requests to 20 req/min per IP.")
                    .recoveryProtocol("Excess traffic receives HTTP 429 Retry-After header; inventory protected by SELECT FOR UPDATE row locks.")
                    .dataConsistencyGuarantee("100% zero overselling guarantee; inventory never decrements below 0.")
                    .build()
    );

    @Override
    public List<InterviewQuestionAnswer> getInterviewChecklist() {
        return checklist;
    }

    @Override
    public SystemArchitectureDiagram getArchitectureDiagram() {
        Map<String, String> dataFlow = new LinkedHashMap<>();
        dataFlow.put("1. Ingress", "Client Browser/App -> CloudFront CDN / AWS ALB (Port 443 TLS)");
        dataFlow.put("2. Routing", "ALB -> Ingress Controller -> Nexora Microservices / Pods (Port 8080)");
        dataFlow.put("3. Caching", "Pods -> Redis Cluster (Port 6379) for Session, Cart & Token Bucket Rate Limit");
        dataFlow.put("4. Search", "Pods -> Elasticsearch Cluster (Port 9200) for sub-second catalog search");
        dataFlow.put("5. Persistence", "Pods -> MySQL Primary (Port 3306) with ACID Transactions & Row-Level Locks");
        dataFlow.put("6. Asynchronous Bus", "Pods -> Apache Kafka (Port 9092) for OrderCreated / Delivery events");
        dataFlow.put("7. Real-Time", "Pods -> STOMP WebSocket Clients for Live Order Tracking updates");

        return SystemArchitectureDiagram.builder()
                .architectureType("Distributed Microservices Architecture with Clean Monolith Foundation")
                .description("Production-grade architecture designed for high availability, zero inventory overselling, and sub-second latency.")
                .clientLayers(Arrays.asList("Web Browser (Modern Commerce UI)", "Mobile React Native Client", "Delivery Rider App"))
                .gatewayLayers(Arrays.asList("AWS Application Load Balancer (ALB)", "Nginx Ingress Controller", "Spring Cloud API Gateway"))
                .applicationServices(Arrays.asList("Auth & User Service (:8081)", "Product Catalog Service (:8082)", "Inventory Service (:8083)", "Order Service (:8084)", "Payment Service (:8085)", "Delivery Tracking Service (:8086)"))
                .asynchronousMessaging(Arrays.asList("Apache Kafka (Topics: nexora.order-events, nexora.delivery-events)", "Dead Letter Queue (DLQ)"))
                .datastores(Arrays.asList("Amazon RDS MySQL 8.0 Multi-AZ", "Amazon ElastiCache Redis 7.2", "Elasticsearch 8.11", "Amazon S3 Media Storage"))
                .dataFlowSummary(dataFlow)
                .build();
    }

    @Override
    public ScaleToMillionBlueprint getScaleToMillionBlueprint() {
        Map<String, String> tiers = new LinkedHashMap<>();
        tiers.put("Tier 1 (10,000 DAU)", "Single Spring Boot instance + MySQL db.t4g.small + Redis standalone. Baseline RPS: ~50.");
        tiers.put("Tier 2 (100,000 DAU)", "Kubernetes 3-5 Pods + RDS Multi-AZ db.t4g.medium + Redis Cluster + Kafka. Baseline RPS: ~450.");
        tiers.put("Tier 3 (1,000,000 DAU)", "Kubernetes 20-50 Pods + Database Sharding + 3 Read-Replicas + Multi-Region CloudFront CDN. Peak RPS: ~4,500.");

        return ScaleToMillionBlueprint.builder()
                .targetScale("1,000,000 Daily Active Users (DAU)")
                .dailyActiveUsers(1000000)
                .peakRpsEstimate(4500.0)
                .scalingTiers(tiers)
                .databaseScalingTactics(Arrays.asList(
                        "Split Reads and Writes: Route all read queries to 3 MySQL Read-Replicas via read-write connection routing.",
                        "Database Sharding: Hash-partition orders and user tables by store_id and user_id across multiple DB instances.",
                        "Connection Pool Tuning: Configure HikariCP with max-pool-size=30 per pod; use AWS RDS Proxy for connection multiplexing."
                ))
                .cachingStrategy(Arrays.asList(
                        "Redis Cluster: 3 master nodes + 3 replica nodes with consistent hashing.",
                        "Sub-millisecond Session & Cart State: Offload 85% of queries from MySQL to in-memory Redis keys.",
                        "Cache Warming: Proactively warm trending store and flash sale product caches 10 minutes prior to sale start."
                ))
                .computeAutoScaling(Arrays.asList(
                        "Kubernetes HPA scales ECS/EKS pods from 5 baseline to 50 pods on 70% CPU threshold within 15 seconds.",
                        "Multi-AZ Deployment: Equal pod distribution across 3 availability zones (ap-south-1a, 1b, 1c) prevents single point of failure."
                ))
                .storageAndCdnStrategy(Arrays.asList(
                        "Amazon CloudFront Edge Caching: Caches product images and static UI at edge locations nearest to customers.",
                        "Origin Access Control (OAC): S3 bucket remains private; only CloudFront edge points can read assets."
                ))
                .build();
    }

    @Override
    public List<FailureScenarioMatrix> getFailureScenarios() {
        return failureScenarios;
    }
}
