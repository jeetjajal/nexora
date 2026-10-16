# NEXORA — Phase 15: Redis In-Memory Caching & Rate Limiting

Phase 15 introduces **Redis (Remote Dictionary Server)** into Nexora to accelerate high-frequency read operations and protect APIs from abuse.

---

## 1. What is Redis in Simple Words?

Imagine keeping a quick-reference notebook on top of your desk:
- **MySQL (Hard Drive / SSD)**: Like an enormous multi-tier filing cabinet in the basement archive. It is permanent, highly structured, ACID-compliant, and durable. But retrieving a folder requires walking to the basement, unlocking drawers, and scanning indexes.
- **Redis (In-Memory RAM)**: Like a handy memo pad directly on your desk. Reading from and writing to computer RAM takes **nanoseconds to microseconds**, whereas disk/network queries take milliseconds.
- **Why Both?**: Redis holds temporary, hot data that millions of people access simultaneously (like product cards, store hours, and active carts). **MySQL remains the single permanent source of truth.** If Redis is restarted or emptied, nothing is lost because data can be re-fetched from MySQL immediately.

---

## 2. Core Redis Concepts Implemented

### 2.1. The Cache-Aside (Lazy Loading) Pattern
```
 [Client API Request]
         │
         ▼
 ┌───────────────┐
 │ Check Redis?  │
 └───────────────┘
    │         │
    │ (Hit)   │ (Miss)
    │         ▼
    │   ┌───────────────┐
    │   │ Query MySQL   │
    │   └───────────────┘
    │         │
    │         ▼
    │   ┌───────────────┐
    │   │ Cache in Redis│
    │   └───────────────┘
    ▼         │
 [Return Response] ◄──┘
```
1. Application receives a read request (e.g. `GET /products/{id}`).
2. Application checks Redis first (`nexora:product:{id}`).
3. **Cache Hit**: Value exists in Redis -> returns immediately in sub-millisecond time.
4. **Cache Miss**: Key does not exist in Redis -> queries MySQL, stores the result in Redis with a TTL, and returns the response.

### 2.2. Time-To-Live (TTL)
- Every cached item has an expiration timestamp (TTL). Once the TTL expires, Redis automatically purges the key.
- **Configured TTLs in Nexora**:
  - `stores`: 15 minutes (`900s`)
  - `products`: 10 minutes (`600s`)
  - `cart`: 30 minutes (`1800s`)
  - `rate-limit`: 1 minute (`60s`)

### 2.3. Cache Invalidation (Eviction on Write)
- Whenever a resource is updated or deleted in MySQL, its corresponding Redis key is explicitly invalidated (`redisTemplate.delete(key)`).
- Examples:
  - When a Store Owner calls `PUT /stores/{id}`, the cache key `nexora:store:{id}` is evicted. Next read will fetch the fresh updated store from MySQL.
  - When items are added or removed from a Cart, `nexora:cart:user:{userId}` is evicted.
  - When an order checkout finishes, the user's cart cache is evicted.

---

## 3. High-Performance API Rate Limiter
Implemented via `RedisRateLimiterService`:
- Uses atomic Redis `INCR` and `EXPIRE`.
- Counts requests per client (e.g., user ID or IP address) within a sliding or fixed time window (e.g. 60 requests per 60 seconds).
- Protects login, checkout, and inventory endpoints from brute-force attacks and bot scrapers.
- **Fail-Open Design**: If Redis connection ever drops, rate limiting logs a warning and allows the request through so legitimate users are never locked out of checkout.

---

## 4. Resilient Architecture & Fallback
In production, Redis might restart, fail over, or suffer network partitions.
Nexora employs a **Resilient Cache Error Handler**:
- If Redis is down, `RedisCacheService` and `RedisConfig` catch connection exceptions gracefully and execute the MySQL query directly.
- The user experiences only standard database latency, never an application crash (`500 Internal Server Error`).

---

## 5. Verification & Test Summary
The Phase 15 test suite verifies:
- `RedisCacheServiceTest`: Tests Cache-Aside hit/miss flows, TTL registration, key deletion, and exception fallback.
- `RedisCartCacheServiceTest`: Tests user cart caching, retrieval, and eviction.
- `RedisRateLimiterServiceTest`: Tests atomic incrementing, window expiration, limit enforcement, and fail-open behavior.
- Existing tests across Phases 1–14 remain 100% functional.
