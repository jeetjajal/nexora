# NEXORA — Phase 8: Order Management

Continues directly from Phases 1-7. Nothing from any earlier phase was
rewritten. This phase touched three existing files, all additively:

- `GlobalExceptionHandler` — three new `@ExceptionHandler` methods (same
  pattern every earlier phase used).
- `InventoryService` (Phase 6) — two new methods, `reduceStockForOrder`
  and `restoreStockForOrder` (see section 3).
- `ProductService` (Phase 4) — one new guard added to `deleteProduct`
  (see section 6).

Everything else is new: a small `address` service/controller layer
(a prerequisite gap identified before writing any order code — see
section 1), and the full `order` package.

Still no Redis, Kafka, AI, payments, microservices, or Docker. Payment
integration (Phase 9/10) doesn't exist yet — orders are created
directly in `CREATED` status; see section 4's note on `PAID`.

---

## 1. Prerequisite gap found and filled: Address CRUD

Checkout needs a `deliveryAddressId`. Phase 2 built the `Address`
entity, repository, DTO, and mapper as groundwork — but never a
service or controller, because nothing needed one yet. Without an
API, no customer could ever have a saved address to reference.

Added, following the exact ownership pattern Cart (Phase 7)
established (every operation resolves "the caller's own" from the JWT,
no admin override needed for personal data):

| Method | Path |
|---|---|
| POST | `/api/v1/addresses` |
| GET | `/api/v1/addresses` |
| GET | `/api/v1/addresses/{id}` |
| PUT | `/api/v1/addresses/{id}` |
| DELETE | `/api/v1/addresses/{id}` |

`AddressService.findOwnedAddressOrThrow` is `public` specifically so
`OrderService` can reuse the exact same ownership check during
checkout instead of duplicating it.

---

## 2. What was built (Order module)

```
src/main/java/com/nexora/order/
├── entity/
│   ├── Order.java         <- one per STORE per checkout (see section 5)
│   ├── OrderItem.java     <- SNAPSHOTS price + name (contrast with CartItem)
│   └── OrderStatus.java   <- the 12-status enum from the roadmap
├── repository/
│   ├── OrderRepository.java
│   └── OrderItemRepository.java
├── dto/
│   ├── CheckoutRequest.java
│   ├── OrderStatusUpdateRequest.java
│   ├── OrderItemResponse.java
│   └── OrderResponse.java
├── mapper/
│   └── OrderMapper.java
├── exception/
│   ├── EmptyCartException.java
│   └── InvalidOrderStatusTransitionException.java
├── service/
│   └── OrderService.java   <- checkout + the state machine
└── controller/
    └── OrderController.java
```

New MySQL tables: `orders`, `order_items` — matching names already
anticipated in the original database design (Phase 1).

---

## 3. Checkout — how a Cart becomes real Orders

`POST /api/v1/orders/checkout` — request body is just
`{ "deliveryAddressId": 12 }`. Everything else comes from the
caller's own Cart (Phase 7) and current `Product`/`Inventory` data.

Steps, all inside **one `@Transactional` method**:

1. Resolve and ownership-check the delivery address.
2. Load the cart; reject with `400 EmptyCartException` if empty.
3. Confirm every product in it is still available (a product could
   have been deactivated by its store after being added to the cart —
   Phase 7 only checked this at add-time).
4. **Group cart items by store.** A cart can span multiple stores
   (Phase 7 never restricted that); a real order is always per-store
   — see section 5 for why. Checkout creates one `Order` per distinct
   store represented in the cart.
5. For each store's items: snapshot price and name into a new
   `OrderItem`, and **actually consume stock** via
   `InventoryService.reduceStockForOrder` — the same atomic,
   concurrency-safe `UPDATE ... WHERE quantity >= :amount` operation
   Phase 6 built for exactly this purpose.
6. Only after every store group succeeds, clear the cart.

**If any single item anywhere fails** (insufficient stock, most
commonly), the whole transaction rolls back — no partial orders, no
partially-decremented stock. This is a deliberately simple, safe
choice over a more complex partial-success design.

### Why a new pair of InventoryService methods, instead of reusing `reduceStock`/`increaseStock` (Phase 6)?

Phase 6's `reduceStock`/`increaseStock` are STORE_OWNER/ADMIN
**management** actions, gated by "do you own this product's store?".
During checkout, the caller is a **customer buying something** — a
completely different authorization question, already answered by
`OrderService` deciding "this is a valid checkout for this customer's
own cart." `reduceStockForOrder`/`restoreStockForOrder` reuse the
exact same atomic repository operations, just without that
store-ownership check, and are not exposed as their own controller
endpoints — only `OrderService` calls them.

---

## 4. The order state machine

```
CREATED ────────► PAYMENT_PENDING ────► PAID ────► CONFIRMED ────► PREPARING ────► READY
   │                    │                 │             │               │
   ▼                    ▼                 ▼             ▼               ▼
CANCELLED           CANCELLED         CANCELLED     REJECTED       CANCELLED
                                       REJECTED      CANCELLED

READY ──► RIDER_ASSIGNED ──► PICKED_UP ──► OUT_FOR_DELIVERY ──► DELIVERED
```

`DELIVERED`, `CANCELLED`, and `REJECTED` are **terminal** — no
transition is ever allowed out of them again (this is exactly what
blocks the roadmap's own example, `DELIVERED -> PREPARING`).

Enforced by `OrderService.ALLOWED_TRANSITIONS`, a
`Map<OrderStatus, Set<OrderStatus>>` — any transition not explicitly
listed is rejected with `409 InvalidOrderStatusTransitionException`.

**Two separate endpoints, two separate authorization rules:**

| Endpoint | Who | What |
|---|---|---|
| `PATCH /api/v1/orders/{id}/status` | STORE_OWNER (of that order's store) or ADMIN | Full state machine — any valid transition |
| `POST /api/v1/orders/{id}/cancel` | The customer who placed it, or ADMIN | Only `CANCELLED`, and only from `CREATED`/`PAYMENT_PENDING` |

The narrower customer endpoint reflects reality: once a store has
started `PREPARING` an order, cancellation becomes the store's call
(via the general endpoint), not something a customer can unilaterally
do through self-service.

**On `PAYMENT_PENDING`/`PAID`:** Phase 9/10 (Payment) doesn't exist
yet, so for now these are reachable through the same general-purpose
endpoint as everything else. Once real payment integration exists,
`PAID` in particular should only ever be set by a verified payment
webhook, not manually — that tightening is explicitly deferred to
Phase 9/10, not forgotten.

**Cancelling or rejecting an order restores stock** — `updateStatus`
and `cancelOwnOrder` both call `restoreStockForOrder` (which loops
every `OrderItem` and calls `InventoryService.restoreStockForOrder`)
whenever the target status is `CANCELLED` or `REJECTED`, since those
mean the units are no longer being fulfilled.

---

## 5. Why one Order per store (not one Order for the whole cart)

A cart can span multiple stores; a real fulfillment can't — one
restaurant prepares and hands off one package, it can't jointly
fulfill an order that's half groceries from one store and half pizza
from another. Checkout splits accordingly, and this also matches the
`orders` table's original design (a single `store_id` column per
order, from Phase 1's database brief). `POST /orders/checkout` returns
a **list** of `OrderResponse` — usually one, but as many as the
distinct stores represented in the cart.

---

## 6. Price snapshotting — the key difference from Cart

Cart (Phase 7) always shows **live** prices — nothing's been bought
yet. `OrderItem` does the opposite: it snapshots `unitPrice` and
`productNameSnapshot` **at the moment of purchase**, into real columns
that are never recomputed. If a store later changes a product's price
or name, every past order still shows exactly what the customer
actually paid for exactly what they actually ordered.

**Interaction with Phase 4's product deletion:** once a product has
real order history, hard-deleting it (`DELETE /api/v1/products/{id}`)
would either orphan that history or fail with a raw foreign-key
violation. `ProductService.deleteProduct` now checks
`OrderItemRepository.existsByProductId` first and returns a clean
`409 ProductInUseException` pointing to `deactivateProduct` instead —
this situation simply couldn't occur before Phase 8, since no
`OrderItem` table existed.

---

## 7. Endpoints

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/orders/checkout` | any authenticated (checks out their own cart) |
| GET | `/api/v1/orders` | any authenticated — own order history, paginated |
| GET | `/api/v1/orders/{id}` | owning customer, owning store's owner, or ADMIN |
| POST | `/api/v1/orders/{id}/cancel` | owning customer or ADMIN, only from CREATED/PAYMENT_PENDING |
| PATCH | `/api/v1/orders/{id}/status` | STORE_OWNER (of that order's store) or ADMIN |
| GET | `/api/v1/stores/{storeId}/orders` | STORE_OWNER (of that store) or ADMIN, paginated |

---

## 8. Run the Phase 8 tests

```bash
mvn test
```

Two new test classes:

1. **`OrderStatusTransitionTest`** (Mockito unit tests) — every valid
   transition in the happy-path chain, the roadmap's own
   `DELIVERED -> PREPARING` rejection example, all three terminal
   statuses parametrically confirmed to allow nothing further,
   state-skipping rejection, store-ownership authorization on the
   status endpoint, and customer self-cancel (success while `CREATED`,
   rejection once `PREPARING`, stock restoration verified).

2. **`OrderCheckoutIntegrationTest`** (full `@SpringBootTest` +
   `MockMvc`, real Spring Security chain) — the complete flow:
   - Address CRUD prerequisite works.
   - Checkout creates an order, **really** reduces stock (verified by
     re-reading `Inventory` from the database), and clears the cart.
   - Empty cart → `400`; someone else's address id → `403`.
   - **Exceeding available stock rolls back the entire checkout** —
     verified by confirming zero orders exist afterward, not just that
     the HTTP call failed.
   - **A cart spanning two different stores checkout into two separate
     orders.**
   - **Price snapshotting proven directly**: place an order, change
     the product's price afterward, confirm the already-placed order
     still shows the original price.
   - Order visibility: owning customer yes / other customer no; owning
     store's owner yes / other store owner no; ADMIN always yes.
   - Status transitions via the real API, including the `409` for an
     invalid jump and `403` for a customer trying to use the
     store-side endpoint.
   - Customer self-cancel restores stock (verified via a real
     before/after `Inventory` read), and is rejected once the store has
     advanced the order to `PREPARING`.
   - A product referenced by a real order cannot be hard-deleted.

These run alongside every earlier phase's suite in the same `mvn test`
invocation, so nothing from Phases 1-7 is skipped as regression coverage.

---

## 9. Common errors & fixes (Phase 8 additions)

| Error | Cause | Fix |
|---|---|---|
| `400 Cannot checkout: your cart is empty` | Nothing in the cart, or cart never created | `POST /api/v1/cart/items` first |
| `403` on checkout | The `deliveryAddressId` belongs to a different user | `GET /api/v1/addresses` to see your own saved addresses and their ids |
| `409` on checkout | A product's stock ran out between add-to-cart and checkout | Expected behavior — see Phase 7 docs on carts not reserving stock; re-check the cart and retry |
| `409` on a status update | The requested transition isn't allowed from the order's current status | See the state-machine diagram in section 4 |
| `409` deleting a product | It's referenced by a real order | Use `PATCH /api/v1/products/{id}/deactivate` instead |
| Order total looks "wrong" after a price change | Working as intended — orders snapshot price at checkout time, they never reflect later product changes | Check the *product's* current price separately if needed; the order's price is historically accurate on purpose |

---

## 10. What's next

**Phase 8 is complete. Stopping here, as instructed.**

Phase 9 (per the roadmap) integrates Razorpay Test Mode: real
payment-order creation, connecting checkout to an actual payment flow,
and — as flagged in section 4 — tightening who's allowed to set an
order's status to `PAID` (a verified webhook, not a manual store-owner
action). Phase 10 then adds signature verification and webhook
handling on top of that.

Say the word when you're ready to move to **Phase 9**.
