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

## 7. Known Production Considerations & Next Steps

1. **Schema Migration Tooling:** Migrate from Hibernate `ddl-auto=update` to Flyway/Liquibase for repeatable, version-controlled zero-downtime database migrations.
2. **External Notification Credentials:** When moving beyond sandbox simulation, configure production SMTP and SMS API keys in Vercel and backend environments.
