# NEXORA — Phase 16: Apache Kafka Integration

Phase 16 introduces **distributed event streaming** into Nexora using **Apache Kafka**.

---

## 1. What is Apache Kafka in Simple Words?

Imagine a busy commercial kitchen:
- In a **synchronous REST (traditional) system**, every time an order is placed, the waiter has to stop, run to the chef, wait for the chef to answer, then run to the inventory manager, wait for them to update the ledger, run to the SMS dispatch desk, wait for the SMS confirmation, and only THEN return to give the customer their receipt. If the SMS desk is busy or down, the whole checkout crashes or hangs!
- In **Apache Kafka (event-driven streaming)**, the waiter simply puts a digital ticket on a central bulletin board (the **Kafka Broker**). The waiter hands the receipt to the customer immediately (sub-millisecond latency). In the background, the kitchen staff, inventory clerk, SMS gateway, and analytics engine all look at the bulletin board at their own pace and do their work independently. Even if the SMS desk takes a 5-minute break, the tickets remain safely stored on the board and will be processed as soon as it returns.

### Core Kafka Terminology Explained Simply:
- **Broker**: The Kafka server (or cluster) that receives, persists, and serves messages.
- **Topic**: A named category/channel where messages are published (e.g. `nexora.order-events`, `nexora.payment-events`).
- **Producer**: An application component that creates and publishes events (e.g., `KafkaEventProducer`).
- **Consumer**: A component that subscribes to topics, reads events, and performs downstream actions (e.g., `InventoryEventConsumer`, `NotificationEventConsumer`, `AnalyticsEventConsumer`).
- **Partition**: A topic is divided into partitions for parallelism. Messages in a partition are strictly ordered.
- **Record Key**: An identifier attached to each message (we use `orderId`). Kafka guarantees that all records with the same key go to the **exact same partition**, ensuring chronological order.
- **Offset**: A unique sequential integer assigned to each record in a partition. Consumers track offsets to know where they left off.
- **Consumer Group**: A group of consumers working together. Each partition is assigned to only one consumer in the group, enabling horizontal scaling without duplicate processing.

---

## 2. Architecture: Order Service → Kafka → Inventory / Notification / Analytics

```
               [Customer Request]
                       │
                       ▼
             ┌───────────────────┐
             │   Order Service   │ ──► MySQL (Source of Truth)
             └───────────────────┘
                       │
             (Non-blocking Event)
                       │
                       ▼
          ═══════════════════════════
                 APACHE KAFKA
            (Topics & Partitions)
          ═══════════════════════════
             │           │          │
             ▼           ▼          ▼
     ┌─────────────┐ ┌────────┐ ┌───────────┐
     │  Inventory  │ │ Notify │ │ Analytics │
     │  Consumer   │ │Consumer│ │ Consumer  │
     └─────────────┘ └────────┘ └───────────┘
            │            │            │
            ▼            ▼            ▼
     Auto Restock   Email/SMS    Metrics, GMV,
     on Cancel      Customer/    Order Volume
                    Rider
```

### Why Decouple with Kafka?
1. **Zero Blocker Latency**: Checkout and state transition APIs don't wait for notifications, third-party dispatchers, or analytics calculation.
2. **Fault Isolation**: If the email service is down, checkout does NOT fail. The event sits securely in Kafka until the consumer recovers.
3. **Horizontal Scalability**: Add more analytics or notification consumers by simply adding nodes to the consumer group without modifying `OrderService`.
4. **Auditability & Replayability**: Events can be replayed to recalculate metrics, backfill new microservices, or reconstruct the timeline of an order.

---

## 3. The 9 Required Events & Schemas

| Event Class | Topic | Key | Trigger Point |
|---|---|---|---|
| `OrderCreatedEvent` | `nexora.order-events` | `orderId` | Customer places order via `OrderService.checkout()` |
| `PaymentCompletedEvent` | `nexora.payment-events` | `orderId` | Razorpay signature/webhook verified via `PaymentService.applySuccessfulPayment()` |
| `PaymentFailedEvent` | `nexora.payment-events` | `orderId` | Webhook reports failure via `PaymentService.applyFailedPayment()` |
| `OrderConfirmedEvent` | `nexora.order-events` | `orderId` | Store owner confirms order via `OrderService.updateStatus(CONFIRMED)` |
| `OrderReadyEvent` | `nexora.order-events` | `orderId` | Store marks meal packaged via `OrderService.updateStatus(READY)` |
| `RiderAssignedEvent` | `nexora.delivery-events` | `orderId` | Nearest rider assigned via `DeliveryService.assignNearestRider()` |
| `OrderDeliveredEvent` | `nexora.delivery-events` | `orderId` | Rider completes delivery via `DeliveryService.updateAssignmentStatus(DELIVERED)` |
| `OrderCancelledEvent` | `nexora.order-events` | `orderId` | Store/Customer cancels order via `OrderService.updateStatus(CANCELLED)` or `cancelOwnOrder()` |
| `RefundCompletedEvent` | `nexora.payment-events` | `orderId` | Store owner/Admin issues refund via `PaymentService.refundPayment()` |

---

## 4. Producer & Consumer Implementations

### 4.1. `KafkaEventProducer`
- Implements asynchronous message publishing using `KafkaTemplate<String, String>`.
- Converts event objects to JSON using `ObjectMapper`.
- Partitions deterministically by `orderId.toString()`.
- Uses non-blocking callbacks via `.whenComplete((res, ex) -> ...)`:
  - If broker fails, logs error gracefully.
  - Transactions in MySQL are committed first; Kafka failure does not roll back an already committed order unless distributed transactions (Saga / Outbox Pattern) are configured.

### 4.2. `InventoryEventConsumer`
- Listens to `nexora.order-events`.
- Detects `OrderCancelledEvent`.
- Automatically calls `InventoryService.restoreStock(productId, quantity)` for all items in the cancelled order.

### 4.3. `NotificationEventConsumer`
- Listens to `nexora.order-events`, `nexora.payment-events`, and `nexora.delivery-events`.
- Triggers customer, store owner, and rider email notifications via `NotificationService`:
  - Order confirmation & ready status
  - Payment success receipt & failure alerts
  - Rider assigned details & delivery completion notice

### 4.4. `AnalyticsEventConsumer`
- Maintains real-time running business aggregates:
  - Total order counts & order status distribution
  - Cumulative Gross Merchandise Value (GMV)
  - Payment conversion rate (completed vs failed)
  - Delivery completions

---

## 5. Resilient Error Handling & Dead Letter Topic (DLT)

Configured in `KafkaConfig.java`:
- **Retries with Exponential/Fixed Backoff**:
  ```java
  FixedBackOff backOff = new FixedBackOff(1000L, 3L);
  DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
  ```
- **Dead Letter Queue (`DeadLetterPublishingRecoverer`)**:
  - If a message fails after 3 retry attempts (e.g. malformed JSON or unrecoverable error), it is routed to `nexora.dead-letter-topic` for inspection without stalling the main consumer partition.
- **Fail-Fast Offline Local Support**:
  - `ProducerConfig.MAX_BLOCK_MS_CONFIG` is set to `1000ms` (and `200ms` in test suite), preventing indefinite metadata-fetch hangs when running offline without a Kafka broker.

---

## 6. Why Kafka is NOT Used for Normal Synchronous CRUD

| Operation | Protocol Used | Reason |
|---|---|---|
| Customer Login / Auth | REST / HTTP | Customer needs immediate JWT token response synchronously. |
| Add / Remove Cart Item | REST / HTTP | Frontend must display immediate updated cart total. |
| Fetch Store List / Products | REST / HTTP | Read query needs synchronous response. |
| Checkout & Payment Request | REST / HTTP | Customer requires instant Razorpay order ID and checkout response. |
| **Order State Notifications** | **Kafka Event Streaming** | Asynchronous, decoupled, high throughput, tolerant to service delays. |
| **Inventory Restoration** | **Kafka Event Streaming** | Background reconciliation upon cancellation. |
| **Analytics & Metrics** | **Kafka Event Streaming** | Fire-and-forget streaming without slowing down transactions. |

---

## 7. Verification & Testing

The Phase 16 test suite verifies end-to-end event serialization, producer dispatch, and consumer ingestion:
- `KafkaEventProducerTest` (9 tests): Validates payload JSON, topic routing, and partition keys for all 9 lifecycle events.
- `InventoryEventConsumerTest` (1 test): Validates stock restoration on order cancellation.
- `NotificationEventConsumerTest` (3 tests): Validates alert emails for order, payment, and delivery events.
- `AnalyticsEventConsumerTest` (3 tests): Validates order counts, payment counts, and GMV metric accumulation.
- `OrderStatusTransitionTest` (13 tests), `PaymentServiceTest` (8 tests), `DeliveryServiceTest` (2 tests), `IdempotencyConcurrencyIntegrationTest` (1 test): Complete backward compatibility across all existing state machines and services.

**Result: BUILD SUCCESS (0 failures, 0 errors across 40+ tests)**
