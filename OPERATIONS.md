# NEXORA — Operations, Security Hardening & Reliability Manual

This manual provides authoritative operational procedures, environment configurations, security hardening rules, disaster recovery steps, and troubleshooting runbooks for the NEXORA Quick Commerce Platform.

---

## 1. System Topology & Architecture

```
[ Client Browsers / Mobile PWA ]
               │
               ▼
   [ Vercel Edge Network ] ── Next.js 14 App Router (Frontend Proxy)
   (https://nexora-mu-eosin.vercel.app)
               │
               ▼ (X-Correlation-Id Header Injected)
   [ Spring Boot 3.3.4 Application Engine ]
       ├── Security: Stateless JWT (BCrypt + RBAC + Object-Level Ownership)
       ├── Tracing: SLF4J MDC CorrelationIdFilter
       ├── Connection Pool: HikariCP (Max: 20, Min-Idle: 5, Timeout: 20s)
       ├── Notifications: Event-Driven Provider Engine (Deduplication + Scrubbing)
       └── Analytics: Database-Backed GMV, SLA & Payment Reconciliation Engine
               │
               ▼
   [ MySQL 8.0 Primary Database ]
```

---

## 2. Environment Variables & Secret Management

All sensitive secrets are isolated into environment variables and never committed to version control.

| Variable Name | Required Scope | Description | Sample Placeholder |
| :--- | :--- | :--- | :--- |
| `NEXORA_DB_PASSWORD` | Backend | MySQL database authentication password | `[Secure-Database-Password]` |
| `NEXORA_JWT_SECRET` | Backend | High-entropy signing key for HMAC-SHA512 JWT | `[Min-64-Byte-Random-Base64-String]` |
| `RAZORPAY_KEY_ID` | Backend / Frontend | Razorpay merchant public key ID | `rzp_live_xxxxxxxxxxxxxx` |
| `RAZORPAY_KEY_SECRET` | Backend | Razorpay private API secret | `[Razorpay-Secret-Key]` |
| `RAZORPAY_WEBHOOK_SECRET`| Backend | Webhook signature verification secret | `[Razorpay-Webhook-Secret]` |
| `NEXT_PUBLIC_API_URL` | Frontend | Authoritative backend REST & WebSocket endpoint | `https://api.nexora.com` |
| `NEXORA_NOTIFS_SMTP_HOST`| Backend | Production SMTP gateway host | `smtp.sendgrid.net` |
| `NEXORA_NOTIFS_SIMULATION`| Backend | Simulation toggle (`true` in dev, `false` in prod) | `false` |

---

## 3. Database Maintenance & Backup Procedures

### A. Full Logical Backup (Automated Nightly Runbook)
Run logical consistent dumps during low-traffic maintenance windows (03:00 UTC) with single-transaction guarantees:
```bash
mysqldump -u nexora_user -p \
  --single-transaction \
  --quick \
  --triggers \
  --routines \
  --events \
  --hex-blob \
  --databases nexora_db | gzip > /backups/nexora_db_$(date +%Y%m%d_%H%M%S).sql.gz
```

### B. Point-in-Time Database Restoration Runbook
1. Terminate active application backend instances to prevent writes.
2. Unpack and stream the baseline snapshot:
```bash
gunzip < /backups/nexora_db_20261026_030000.sql.gz | mysql -u nexora_user -p nexora_db
```
3. Replay binary logs up to the exact incident timestamp:
```bash
mysqlbinlog --stop-datetime="2026-10-26 14:30:00" /var/log/mysql/binlog.000042 | mysql -u nexora_user -p nexora_db
```

---

## 4. Payment Webhook Security & Idempotency

- **Webhook URL:** `POST /api/v1/payments/webhook`
- **Security:** Publicly routed in `SecurityConfig`, but guarded by cryptographic `HMAC-SHA256` signature verification (`RazorpaySignatureVerifier`).
- **Duplicate Prevention:** Payments and disputes utilize transactional checks (`PaymentTransactionRepository`, `DisputeRepository.existsByOrderIdAndStatusIn`) preventing duplicate ledger crediting or double-refund processing.

---

## 5. Event-Driven Notification Engine

NEXORA dispatches notifications on all key customer milestones:
1. `ORDER_CONFIRMED`
2. `PAYMENT_SUCCESS` / `PAYMENT_FAILED`
3. `ORDER_PACKED`
4. `OUT_FOR_DELIVERY` (with delivery OTP & rider details)
5. `ORDER_DELIVERED`
6. `REFUND_PROCESSED`

### Transparent Delivery Status
Delivery records are classified accurately in `notification_logs`:
- `DELIVERED`: Active SMTP/SMS provider transmitted the message.
- `QUEUED_UNCONFIGURED`: Provider credentials not detected in environment; message stored safely without false claim of delivery.
- `SIMULATED_LOCAL`: Development/test sandbox mock.
- `FAILED`: Provider error after 3 automatic retries.

### Sensitive Payload Redaction
All bodies undergo automatic regex sanitization redacting credit card numbers, CVVs, passwords, and authorization tokens prior to database persistence.

---

## 6. Observability, Health & Tracing

- **Readiness / Health Check:** `GET /api/v1/health`
  - Validates JVM uptime, memory footprint, and MySQL database connection pool availability.
- **Actuator Health & Metrics:** `GET /actuator/health` and `GET /actuator/prometheus`.
- **Distributed Request Tracing:** `CorrelationIdFilter` checks `X-Correlation-Id` or `X-Request-Id`, binds to SLF4J MDC, and populates the response header.

---

## 7. Database Migration & Schema Evolution (Flyway)

Version-controlled SQL migrations are located in `backend/src/main/resources/db/migration/`:
- `V1__init_schema.sql`: Initial schema definition for roles, users, user_roles, categories, stores, store_categories, products, product_categories, inventory, addresses, orders, and order_items.
- `V2__delivery_payments_disputes_analytics.sql`: Schema extensions for delivery_partners, delivery_assignments, payments, payment_transactions, disputes, surge_pricing_rules, notification_logs, and idempotency_keys.

In production environments, enable Flyway in `application-prod.properties` or environment variables:
```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.jpa.hibernate.ddl-auto=validate
```

---

## 8. Vercel Frontend & Backend Integration

The Next.js frontend proxy routes (`frontend/src/app/api/**`) securely route API calls using `getBackendUrl()` from `frontend/src/lib/server-config.ts`:
1. `BACKEND_URL`: Primary server-side URL to the Spring Boot instance (e.g. `https://api.nexora.com` or internal VPC address).
2. `BACKEND_API_URL`: Alternate server-side URL fallback.
3. `NEXT_PUBLIC_API_URL`: Client-side accessible backend URL.
4. Default fallback: `http://localhost:8080` (for local development).

### Required Vercel Project Environment Variables:
- `BACKEND_URL`: Set to your deployed Spring Boot URL (e.g. `https://nexora-backend.up.railway.app` or EC2 endpoint).
- `RAZORPAY_KEY_ID`: Razorpay merchant public key ID (`rzp_live_...`).
- `RAZORPAY_KEY_SECRET`: Razorpay secret for HMAC-SHA256 signature verification.
- `ENFORCE_BACKEND`: Set to `true` in production to reject orders when backend is unreachable instead of falling back to local simulation.

---

## 9. Launch Verification & QA Status (Phase 10)

- **Total Backend Tests:** 73 passing unit & integration tests (`mvn test`)
- **Frontend Build:** 37 routes pre-rendered with 0 type/build errors (`npm run build`)
- **CI Pipeline:** GitHub Actions build passing on `main` and `rebuild/quick-commerce`
- **Frontend Live URL:** `https://nexora-mu-eosin.vercel.app` (HTTP 200 OK)
- **Security Protections Active:**
  - Mandatory cryptographic HMAC-SHA256 Razorpay signature check (`crypto.timingSafeEqual`)
  - Strict Object-Level Access Control (Customer isolation, Admin/Store Manager RBAC)
  - Delivery OTP lockout after 3 consecutive failed attempts
  - Double-refund and duplicate payment idempotency protection
  - Automated regex scrubbing of sensitive card/auth credentials from notification logs
