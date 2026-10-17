# Nexora — Phase 17: Real-Time Order & Delivery Tracking via Spring WebSocket

## 1. Overview & Architecture

Phase 17 transitions **Nexora** into a reactive, real-time platform by integrating **Spring WebSocket** and the **STOMP (Simple Text Oriented Messaging Protocol)** message broker with SockJS fallback.

Instead of clients continuously polling REST endpoints (`GET /api/v1/orders/{id}`) every few seconds, the backend actively pushes status transitions and live delivery partner telemetry to subscribed clients over persistent full-duplex TCP channels.

```
                              ┌─────────────────────────────────────────┐
                              │            Client Application           │
                              │  (Browser / Mobile / tracking.html)    │
                              └───────▲─────────────────────────▲───────┘
                                      │                         │
                 WebSocket (STOMP)    │                         │  Live GPS Telemetry
               /topic/orders/{id}     │                         │  /topic/orders/{id}/delivery
                                      │                         │
┌─────────────────────────────────────┴─────────────────────────┴────────────────────────────┐
│                                    Nexora Phase 17 Backend                                 │
│                                                                                            │
│   OrderService  ─────────► OrderTrackingWebSocketService ◄───────── DeliveryService       │
│   (State Machine)                      │                               (Rider GPS)        │
│                                        │ SimpMessagingTemplate                             │
│                                        ▼                                                   │
│   Kafka Topics  ─────────► WebSocketOrderEventConsumer                                    │
│   (order-events)          (Kafka-to-WebSocket Bridge)                                      │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. REST vs WebSocket Comparison

| Dimension | REST API (HTTP Polling) | Spring WebSocket (STOMP) |
| :--- | :--- | :--- |
| **Connection Model** | Request-Response (Stateless, Short-lived) | Persistent Full-Duplex TCP Connection |
| **Directionality** | Half-Duplex (Client must pull) | Bi-directional (Server pushes instantly) |
| **Latency** | Polling Interval Delay (e.g. 5–10s) | Instantaneous Sub-Millisecond Dispatch |
| **Overhead** | HTTP Headers & TCP Handshake on every request | 1 TCP Handshake; subsequent frames are minimal text |
| **Scalability** | High network and database load under frequent polling | Event-driven pushes only when state actually changes |
| **Use Case in Nexora** | Initial checkout, CRUD, store management | Live order progress, rider GPS tracking, alerts |

---

## 3. STOMP Topics & Destinations

### Inbound STOMP Endpoints (Client to Server)
- Handshake URL: `http://localhost:8080/ws-nexora` (SockJS & WebSocket supported)
- Application Prefix: `/app`
- Telemetry Ingestion: `/app/orders/{orderId}/location` (Delivery partners stream GPS updates)

### Outbound STOMP Topics (Server to Client)
- `/topic/orders/{orderId}`: Receives `OrderStatusUpdateMessage` when order transitions through lifecycle (`CONFIRMED`, `PREPARING`, `READY`, `RIDER_ASSIGNED`, `PICKED_UP`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`).
- `/topic/orders/{orderId}/delivery`: Receives `DeliveryLocationUpdateMessage` carrying latitude, longitude, speed, remaining distance, and ETA.
- `/topic/users/{userId}/orders`: Personal order notification stream for customer.

---

## 4. REST Endpoints (Phase 17)

- `POST /api/v1/orders/{orderId}/delivery/location`: Alternative REST ingestion for rider GPS updates. Broadcasts over WebSocket.
- `GET /api/v1/orders/{orderId}/tracking`: Fast snapshot query of the current status and location for initial screen rendering.
- `GET /tracking.html`: Interactive live order tracking dashboard.

---

## 5. Verification & Testing

- `OrderTrackingWebSocketServiceTest`: Validates broadcast routing, DTO generation, and in-memory status caching.
- `DeliveryTrackingControllerTest`: Validates REST endpoints and telemetry dispatching.
- `WebSocketOrderEventConsumerTest`: Validates the Kafka event bridge into WebSocket broadcasts.
- `WebSocketConfigTest`: Validates STOMP prefixes and SockJS endpoint registration.
