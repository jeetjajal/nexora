# NEXORA — Phase 6: Inventory Management

Continues directly from Phases 1-5. Builds on Phase 4's inventory
foundation (Inventory entity, view + set-absolute-quantity) - nothing
from Phase 4 was removed; this phase adds the operations real stock
management needs day to day, with the concurrency guarantee the
roadmap calls for.

Still no Redis, Kafka, AI, payments, microservices, or Docker - and
still no distributed locking (that's explicitly Phase 21). What this
phase does guarantee: inventory can never go negative, even under
genuinely simultaneous requests, using nothing more exotic than a
correctly-written SQL UPDATE statement.

---

## 1. What changed, file by file

| File | Change |
|---|---|
| Inventory entity | Added lowStockThreshold (int, default 5) |
| NEW InventoryStatus enum | OUT_OF_STOCK / LOW_STOCK / IN_STOCK - derived, not persisted |
| InventoryRepository | Added reduceStockIfAvailable(...) and increaseStock(...) - atomic conditional UPDATE queries |
| InventoryResponse | Added status and lowStockThreshold fields |
| InventoryMapper | Added computeStatus(...) - derives status fresh from quantity vs threshold every time |
| InventoryService | Added increaseStock, reduceStock, updateLowStockThreshold; updateStock (Phase 4) and getInventoryForProduct/isAvailable kept as-is |
| InventoryController | Added POST .../increase, POST .../reduce, PUT .../threshold; GET/PUT (Phase 4) unchanged |
| NEW StockAdjustmentRequest, LowStockThresholdRequest | Request DTOs for the new operations |
| NEW InsufficientStockException | 409 - the clean failure path when a reduce request can't be satisfied |
| GlobalExceptionHandler | Added one handler for the exception above - additive |

Authorization is unchanged: every mutating inventory endpoint still
requires STORE_OWNER (of that product's store) or ADMIN, enforced
exactly the way Phase 4 established it.

---

## 2. The race condition, explained

Picture a product with exactly 1 unit left in stock, and two customers
who both click "Buy" within the same millisecond.

### The naive approach (what we did NOT do)

```java
Inventory inv = inventoryRepository.findByProductId(id).get(); // read
if (inv.getQuantity() >= amount) {                               // check
    inv.setQuantity(inv.getQuantity() - amount);                 // write
    inventoryRepository.save(inv);
}
```

Run on two threads at once:

```
Thread A: reads quantity = 1
Thread B: reads quantity = 1        (already stale, but B doesn't know that)
Thread A: checks 1 >= 1 -> true, sets quantity = 0, saves
Thread B: checks 1 >= 1 -> true, sets quantity = 0, saves
```

Both requests believe they succeeded. Two orders get created for one
item that existed. This is the classic "lost update" problem: B's
decision was based on data that was already out of date by the time it
made that decision, because the read and the write aren't a single
atomic operation.

### The fix used in Nexora

```java
@Modifying
@Query("UPDATE Inventory i SET i.quantity = i.quantity - :amount, i.updatedAt = :now " +
        "WHERE i.product.id = :productId AND i.quantity >= :amount")
int reduceStockIfAvailable(...);
```

This is one SQL statement that does the check and the write together,
as a single indivisible database operation. The database guarantees
the WHERE quantity >= :amount condition is evaluated against the row's
current committed value at the moment it takes the row's write lock -
not a value some Java code read a moment earlier.

```
Thread A's UPDATE runs first: locks the row, sees quantity=1, writes quantity=0. Commits.
Thread B's UPDATE was waiting for that lock. Once granted, it re-checks
  the condition against the NOW-current value: quantity(0) >= amount(1)?
  -> false. Zero rows match. UPDATE affects 0 rows.
```

InventoryService.reduceStock reads that return value: 1 row affected
means success; 0 means genuinely insufficient stock at that instant,
and it throws InsufficientStockException (409). There is no window
where both threads can believe they succeeded - the database's
row-level locking during the UPDATE statement is what makes this safe,
not any locking code we wrote ourselves.

### What this does and doesn't solve

Does solve: correctness. Inventory mathematically cannot go negative,
no matter how many simultaneous requests hit it, because the database
serializes writes to the same row by design.

Doesn't solve yet: throughput under very heavy contention on a single
hot row - many transactions queuing for the same lock limits how many
requests-per-second one product can absorb. That's a performance
concern, not a correctness one, and it's exactly what Phase 21
explores with @Version-based optimistic locking as an alternative with
different trade-offs (fail fast and retry, instead of queue-and-wait).

---

## 3. Endpoints

| Method | Path | Purpose | Access |
|---|---|---|---|
| GET | /api/v1/products/{id}/inventory | View stock + derived status | any authenticated |
| PUT | /api/v1/products/{id}/inventory | Set to an absolute quantity (Phase 4, e.g. after a manual audit) | owner or ADMIN |
| POST | /api/v1/products/{id}/inventory/increase | Restock - add amount | owner or ADMIN |
| POST | /api/v1/products/{id}/inventory/reduce | Sell/consume - subtract amount, concurrency-safe | owner or ADMIN |
| PUT | /api/v1/products/{id}/inventory/threshold | Configure per-product low-stock threshold | owner or ADMIN |

### Sample: check stock (now with status)

```
GET /api/v1/products/42/inventory
```
```json
{
  "success": true,
  "message": "Inventory fetched successfully",
  "data": {
    "id": 7,
    "productId": 42,
    "quantity": 3,
    "lowStockThreshold": 5,
    "status": "LOW_STOCK"
  }
}
```

### Sample: reduce stock - success

```
POST /api/v1/products/42/inventory/reduce
{ "amount": 1 }
```
200 OK, returns the updated InventoryResponse.

### Sample: reduce stock - insufficient

```json
{
  "success": false,
  "message": "Insufficient stock for product id 42: requested 5 but only 2 available",
  "timestamp": "2026-08-21T10:00:00"
}
```
409 Conflict.

### Sample: restock

```
POST /api/v1/products/42/inventory/increase
{ "amount": 20 }
```

### Sample: adjust low-stock sensitivity

```
PUT /api/v1/products/42/inventory/threshold
{ "threshold": 10 }
```

---

## 4. Why status is computed, not stored

OUT_OF_STOCK / LOW_STOCK / IN_STOCK is never written to the database
as its own column. InventoryMapper.computeStatus(...) derives it fresh
from quantity vs lowStockThreshold every single time a response is
built. If it were stored instead, it would be possible for it to
silently drift out of sync with the real quantity - e.g. if some
future code path updated quantity directly and forgot to also update a
separate status column. Deriving it guarantees it's always correct, at
the trivial cost of one comparison.

---

## 5. Run the Phase 6 tests

```bash
mvn test
```

Two new test classes:

1. InventoryServiceTest (Mockito unit tests) - increase/reduce
   authorization (owner succeeds, a different owner is rejected, ADMIN
   always succeeds), insufficient-stock rejection with the correct
   error message, the exact-zero boundary case, threshold updates, and
   all three derived status values.

2. InventoryConcurrencyIntegrationTest (real @SpringBootTest, genuine
   multi-threading) - this is the test that actually proves the
   guarantee, not just describes it:
   - Stock = 1, 10 real threads fire "reduce stock by 1"
     simultaneously (synchronized to start together via
     CountDownLatch, going through the real Spring-managed
     InventoryService bean, each on its own real database
     transaction) -> exactly 1 succeeds, the other 9 get a clean
     InsufficientStockException, and the final database-committed
     quantity is exactly 0 - never negative.
   - Stock = 5, 20 threads -> exactly 5 succeed, 15 fail, final
     quantity 0.
   - 10 simultaneous increase operations of +10 each -> final quantity
     is exactly 100 (nothing lost to a race, even on the "add" side).

   The test harness bumps H2's LOCK_TIMEOUT and the Hikari connection
   pool size (src/test/resources/application.properties) specifically
   so this burst of real concurrency has enough headroom to run
   cleanly - that's test infrastructure, not a change to how Nexora
   itself behaves.

These run alongside every earlier phase's suite in the same mvn test
invocation - UserServiceTest, RelationshipRepositoryTest,
JwtServiceTest/AuthServiceTest/AuthControllerIntegrationTest,
StoreProductInventoryIntegrationTest, and
SearchFilterSortIntegrationTest all still run as regression coverage
for Phases 1-5.

---

## 6. Common errors & fixes (Phase 6 additions)

| Error | Cause | Fix |
|---|---|---|
| 409 Insufficient stock on a reduce you expected to succeed | Someone else's concurrent request (or an earlier test/manual call) already consumed the stock | Check current quantity via GET .../inventory first |
| increaseStock/reduceStock silently does nothing (0 rows) but no exception in your own code | Called the repository method directly without going through InventoryService, bypassing its existence check | Always go through InventoryService, not the repository directly |
| Concurrency test occasionally slow (not failing, just slow) locally | H2 lock contention under 20 real threads on a constrained machine | Expected - it's proving real serialization is happening; increase the test's timeout if your machine is heavily loaded |
| low_stock_threshold column missing after pulling this phase | App wasn't restarted so Hibernate hasn't run its ddl-auto=update yet | Restart mvn spring-boot:run; existing rows get the Java-side default (5) applied on next write, not automatically backfilled by the ALTER TABLE itself |

---

## 7. What's next

Phase 6 is complete. Stopping here, as instructed.

Phase 7 (per the roadmap) builds the Cart on top of this: add/remove/
update-quantity/clear operations, with the backend always recalculating
prices from the current Product/Inventory state rather than trusting
anything the frontend sends - never trusting a client-supplied price is
the same philosophy this phase applied to never trusting a client's
belief about available stock.

Say the word when you're ready to move to Phase 7.
