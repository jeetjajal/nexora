# NEXORA 3.0 — PHASE 2 DEPLOYMENT & ROLLOUT PLAN

**Module:** Intelligent Delivery Engine (Road-Based Routing, Redis Geospatial Tracking, Confidence ETAs)  
**Target Branch:** `feature/nexora-phase2-intelligent-delivery`  
**Target Environments:** Staging / Production  
**Zero-Downtime Guarantee:** Yes (No breaking schema changes, graceful fallbacks for all external services)

---

## 1. Architecture & Infrastructure Overview

Phase 2 introduces two key distributed infrastructure components:
1. **OSRM (Open Source Routing Machine) Service:** High-performance road-network shortest-path router.
2. **Redis Geospatial Cluster (Redis 6.2+):** In-memory spatial index (`GEOADD`, `GEORADIUS` / `GEOSEARCH`) and ephemeral rider telemetry cache.

```
                    ┌─────────────────────────┐
                    │     Customer / Rider    │
                    │       Next.js App       │
                    └────────────┬────────────┘
                                 │ HTTPS / SSE
                                 ▼
                    ┌─────────────────────────┐
                    │   Spring Boot Backend   │
                    │      (Render Web)       │
                    └──────┬───────────┬──────┘
                           │           │
         Sub-millisecond   │           │ HTTP (Internal/Private)
         Spatial / Cache   │           ▼
               ▼           │    ┌─────────────────────────┐
    ┌────────────────────┐ │    │       OSRM Server       │
    │  Redis Geospatial  │ │    │  (Docker / Standalone)  │
    │ (Upstash / Cloud)  │ │    └─────────────────────────┘
    └────────────────────┘ │
                           ▼ Throttled Durability (1 write / 60s)
                    ┌─────────────────────────┐
                    │    TiDB Cloud MySQL     │
                    │     (nexora_db)         │
                    └─────────────────────────┘
```

---

## 2. Environment Variables & Configuration

### Backend (`application.properties` / Environment)

```properties
# WS1: Routing Configuration
nexora.routing.provider=osrm
nexora.routing.osrm.base-url=${OSRM_BASE_URL:https://router.project-osrm.org}
nexora.routing.osrm.connect-timeout-ms=2000
nexora.routing.osrm.read-timeout-ms=2500
nexora.routing.cache.ttl-seconds=1800
nexora.routing.fallback.average-speed-kmh=25.0

# WS2: Redis Geospatial Rider Telemetry
nexora.rider.telemetry.ttl-seconds=300
nexora.rider.telemetry.stale-threshold-seconds=90
nexora.rider.telemetry.db-throttle-seconds=60

# WS3: ETA Engine Timing Parameters
nexora.eta.store-prep-seconds=180
nexora.eta.dispatch-delay-seconds=60
nexora.eta.handover-seconds=120
```

### OSRM Hosting Recommendations for India / City Clusters

| Deployment Tier | Provider | Description | Estimated Monthly Cost | Latency |
| :--- | :--- | :--- | :--- | :--- |
| **Development / Demo** | Public OSRM Demo | `https://router.project-osrm.org` (subject to rate limiting) | \$0 | 180-350ms |
| **Staging / Initial Production** | Render Private Service | Dockerized OSRM container with pre-extracted city `.osrm` data | \$7 - \$25 | 10-30ms |
| **High Scale Production** | AWS / GCP / DigitalOcean | Dedicated c6i.large (2 vCPU, 4GB RAM) running OSRM India extract | \$35 - \$50 | <10ms |

#### Dockerfile for Private OSRM Deployment:
```dockerfile
FROM osrm/osrm-backend:latest
# Mount pre-processed .osrm files (e.g. gujarat-latest.osrm or india-latest.osrm)
CMD ["osrm-routed", "--algorithm", "mld", "/data/india-latest.osrm"]
```

---

## 3. Redis Sizing & Capacity Estimation

### Telemetry Footprint per Active Rider:
* **Redis GEO element (`ZSET`):** ~64 bytes per rider coordinate
* **Metadata Hash (`nexora:rider:meta:{id}`):** ~320 bytes per rider (lat, lng, speed, heading, timestamp, status)
* **TTL:** 300 seconds (automatic eviction upon disconnect)
* **Total Memory per Rider:** ~400 bytes

### Network Calculation:
* 500 active riders sending telemetry every 5 seconds:
  * Ingestion rate: 100 writes/sec
  * Memory footprint: 500 * 400 bytes = **~200 KB** (negligible)
  * Upstash Redis or AWS ElastiCache t4g.micro (0.5 GB) provides **>2,500x headroom**.

### Connection Pool Protection (HikariCP):
* Without throttling: 100 writes/sec to MySQL = thread pool saturation.
* With Phase 2 durability throttle (max 1 write/60s per rider):
  * MySQL writes: 500 riders / 60s = **~8.3 writes/sec** (reduction of 91.7%).
  * Zero Hikari connection starvation during peak delivery hours.

---

## 4. Rollout Procedure (Zero-Downtime)

### Step 1: Pre-Flight Verification
1. Verify Redis connectivity from Render backend container: `PING -> PONG`.
2. Verify OSRM endpoint availability:
   ```bash
   curl -s "https://router.project-osrm.org/route/v1/driving/72.5611,23.0365;72.5200,23.0300?overview=false"
   ```
3. Run backend and frontend automated test suites:
   * Backend: 67/67 PASS
   * Frontend: 56/56 pages built cleanly.

### Step 2: Staging Deployment
1. Deploy `feature/nexora-phase2-intelligent-delivery` to Staging environment.
2. Ingest mock rider telemetry using `PUT /api/v1/delivery/riders/1/telemetry`:
   ```json
   {
     "latitude": 23.0380,
     "longitude": 72.5630,
     "speedKmh": 25.0,
     "heading": 90.0
   }
   ```
3. Trigger test order checkout and observe:
   * Delivery dispatch assigns nearest rider using Redis Geospatial lookup.
   * `GET /api/v1/orders/{orderId}/tracking` returns delivery window, high confidence score, and road distance.
   * Customer tracking modal renders delivery window and accuracy badge.

### Step 3: Production Rollout
1. Merge reviewed pull request to `main`.
2. Configure Render environment variables (`OSRM_BASE_URL`, `nexora.routing.provider=osrm`).
3. Deploy Spring Boot backend (Render auto-build).
4. Deploy Next.js frontend (Vercel auto-build).
5. Monitor logs for circuit breaker events (`consecutiveFailures`).

---

## 5. Rollback & Failover Strategy

* **OSRM Service Degradation:**
  * If OSRM fails or times out, the built-in circuit breaker trips after 5 failures.
  * The system automatically and silently fails over to `HaversineFallbackRoutingProvider`, flagging orders with `isFallbackEstimate=true` and lowering confidence to `"LOW"`.
  * No customer checkout or order assignment is blocked.
* **Redis Outage Degradation:**
  * If Redis becomes unreachable, `RiderLocationService` catches the exception and falls back to MySQL database queries.
  * Throttling safeguards prevent database overload.
* **Immediate Feature Deactivation:**
  * Setting `nexora.routing.provider=haversine` in Render environment variables disables OSRM routing globally without code changes.
