# 🛍️ NEXORA — Intelligent Food, Grocery & Local-Commerce Platform
> **Complete SDE Portfolio Capstone Project (Phases 1 to 28)**  
> **Tech Stack:** Java 17 • Spring Boot 3.3.4 • MySQL 8 • Redis 7 • Apache Kafka • Elasticsearch 8 • STOMP WebSocket • Gemini AI • Docker Compose • AWS Multi-AZ • Kubernetes HPA • Prometheus & Grafana

---

## 📌 Architectural Overview

Nexora is an enterprise-grade local commerce and food delivery platform designed to solve real-world high-concurrency, distributed reliability, and sub-second latency challenges at scale.

```
                                  [ CloudFront CDN / AWS Route53 ]
                                                 │
                                                 ▼
                                     [ AWS ALB (Public Subnet) ]
                                                 │ (Port 443 / TLS Termination)
                                                 ▼
                              [ NGINX Ingress / Spring Cloud API Gateway ]
                                                 │
                   ┌─────────────────────────────┼─────────────────────────────┐
                   ▼                             ▼                             ▼
       [ Auth & User Service ]       [ Product Catalog Service ]       [ Order & Checkout Service ]
            (Port 8081)                   (Port 8082)                   (Port 8084 - HPA 1 to 5)
                   │                             │                             │
                   ├─────────────────────────────┴─────────────────────────────┤
                   ▼                                                           ▼
       [ Amazon ElastiCache Redis ]                                   [ Distributed Saga Bus ]
       (Cart, Sessions, Rate Limits)                                  (Apache Kafka: 9092)
                   │                                                           │
                   ▼                                                           ▼
       [ Elasticsearch Cluster ]                                   [ MySQL 8 Primary (Multi-AZ) ]
       (Sub-second Inverted Search)                                (Pessimistic Locks & Sharding)
```

---

## 🚀 28-Phase Roadmap Execution Summary

| Phase | Milestone | Engineering Focus & Tech |
|---|---|---|
| **1–5** | **Core Platform Foundation** | Spring Boot 3, MySQL 8 JPA entities, BCrypt + JWT Auth, Store & Category CRUD |
| **6–8** | **Commerce Engine** | Inventory consistency, Cart management, Order Checkout lifecycle |
| **9–11** | **Payment Gateway & Reliability** | Razorpay integration, HMAC-SHA256 signature verification, Idempotency keys |
| **12–14** | **Delivery & Comprehensive Testing** | Nearest-rider dispatch algorithm, responsive dashboards, 300+ unit/integration tests |
| **15–18** | **High-Performance Distributed Layer** | Redis caching (Cache-Aside), Apache Kafka event streaming, WebSocket tracking, Elasticsearch |
| **19–20** | **AI Assistant & Recommendations** | Google Gemini tool-calling (safe backend proxy), hybrid collaborative recommendations |
| **21–22** | **Advanced Concurrency & Microservices** | Pessimistic locking (SELECT FOR UPDATE), 10 decomposed microservices, Distributed Saga Rollback |
| **23–25** | **DevOps, Cloud & Orchestration** | Multi-stage Dockerfile, docker-compose stack, AWS Multi-AZ Terraform, Kubernetes HPA (1 to 5 pods) |
| **26–27** | **Observability & Load Testing** | Prometheus scrapes, Grafana dashboards, P95/P99 latency, k6 multi-scenario load testing |
| **28** | **Final SDE System Design Capstone** | 16 interview answers, 1M users scaling blueprint, failure recovery matrix, 376 tests passing |

---

## 🛡️ SDE Interview Checklist & Core Answers

1. **Spring Boot MVC**: Controller handles HTTP validation (@Valid); Service manages business logic inside @Transactional boundaries; Repository generates optimized SQL via Spring Data JPA.
2. **Zero Overselling under Concurrency**: Inventory purchases execute `SELECT ... FOR UPDATE` with `PESSIMISTIC_WRITE` locks and Redis token bucket rate limiting. 500 simultaneous users buying stock=1 results in exactly 1 purchase and 499 handled conflicts.
3. **Payment Webhook Idempotency**: Razorpay HMAC-SHA256 signatures are verified with constant-time equality check; `payments.razorpay_payment_id` unique database constraints prevent duplicate charges.
4. **Tamper-Proof Totals**: Client submits only product IDs and quantities; backend recalculates subtotal, discounts, and delivery fees server-side from MySQL.
5. **Distributed Saga Rollback**: In microservices checkout, if payment fails at Step 3, compensating transactions automatically reverse stock reservation in Inventory Service and cancel order in Order Service.
6. **Scaling to 1 Million Users**: CloudFront CDN (static/media) + Redis Cluster (85% query offload) + 3 MySQL Read-Replicas + Database Sharding + Kubernetes HPA autoscaling (5 to 50 pods).

---

## 🏃 Quick Start (Local Docker Compose)

```bash
# 1. Clone & prepare environment
git clone https://github.com/jeetjajal/nexora.git
cd nexora/phases/nexora28
cp .env.example .env

# 2. Run the complete multi-container stack
docker compose up -d --build

# 3. Access web dashboards
# - Main App & Store: http://localhost:8080
# - System Design Capstone: http://localhost:8080/system-design.html
# - Performance Benchmarks: http://localhost:8080/loadtesting.html
# - Observability & Metrics: http://localhost:8080/monitoring.html
# - Kubernetes HPA Simulator: http://localhost:8080/kubernetes.html
# - AWS Cloud Explorer: http://localhost:8080/aws.html
# - Docker Containers: http://localhost:8080/docker.html
```
