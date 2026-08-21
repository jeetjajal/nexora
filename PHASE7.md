# NEXORA — Phase 7: Cart

Continues directly from Phases 1-6. A brand new `cart` package - no
existing file from any earlier phase was rewritten, only the shared
GlobalExceptionHandler gained one additive handler for a new exception
type (same pattern every earlier phase used).

Still no Redis, Kafka, AI, payments, microservices, or Docker.

---

## 1. What was built

```
src/main/java/com/nexora/cart/
├── entity/
│   ├── Cart.java              <- one per user, created lazily
│   └── CartItem.java          <- links Cart to Product + quantity (NO price column)
├── repository/
│   ├── CartRepository.java
│   └── CartItemRepository.java
├── dto/
│   ├── AddCartItemRequest.java
│   ├── UpdateCartItemRequest.java
│   ├── CartItemResponse.java
│   └── CartResponse.java
├── mapper/
│   └── CartMapper.java        <- computes every price live from Product
├── exception/
│   └── ProductUnavailableException.java
├── service/
│   └── CartService.java
└── controller/
    └── CartController.java
```

New MySQL tables: `carts`, `cart_items` - both already anticipated in
the original database design list from Phase 1/2.

---

## 2. Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/cart` | View the caller's own cart (created automatically if it doesn't exist yet) |
| POST | `/api/v1/cart/items` | Add a product (or increase its quantity if already present) |
| PUT | `/api/v1/cart/items/{itemId}` | Set a cart item's quantity to an absolute value |
| DELETE | `/api/v1/cart/items/{itemId}` | Remove one item |
| DELETE | `/api/v1/cart` | Clear the entire cart |

Every endpoint requires a valid JWT (Phase 3's `anyRequest().authenticated()`
rule, unchanged) but has **no role restriction** - any authenticated
user has exactly one cart of their own. There is deliberately no
`{cartId}` anywhere in these paths: the cart operated on is always
"whichever cart belongs to the caller," resolved server-side from the
JWT, never supplied by the client.

### Sample: add an item

```
POST /api/v1/cart/items
Authorization: Bearer <token>

{ "productId": 42, "quantity": 2 }
```
```json
{
  "success": true,
  "message": "Item added to cart successfully",
  "data": {
    "id": 7,
    "items": [
      {
        "id": 15,
        "productId": 42,
        "productName": "Margherita Pizza",
        "productImageUrl": "https://example.com/pizza.jpg",
        "unitPrice": 224.10,
        "quantity": 2,
        "lineTotal": 448.20,
        "productAvailable": true
      }
    ],
    "totalItemCount": 2,
    "subtotal": 448.20
  }
}
```

---

## 3. "Never trust the frontend price" - how it's actually enforced

`CartItem` has **no price column at all**. There is nothing for the
frontend to send that would ever be used - `AddCartItemRequest` only
accepts `productId` and `quantity`; even if a client stuffs a `"price"`
field into the JSON body, Jackson silently ignores it (no matching
field exists on the DTO to bind it to).

Every price shown - `unitPrice`, `lineTotal`, cart `subtotal` - is
computed by `CartMapper` **fresh from the current `Product` row**,
every single time the cart is fetched (`unitPrice = product.price -
product.discount`, read live). If a store owner changes a product's
price after it was added to someone's cart, the very next time that
cart is viewed, the new price is what shows up - which is correct,
since nothing has been purchased yet.

This is a different, and simpler, strategy than an `OrderItem` will
use in Phase 8: an order snapshots the price **at the moment of
purchase**, so a later price change never rewrites the history of a
completed order. A cart has no such history to protect - it's meant
to always reflect "what would this cost right now."

---

## 4. Cart validates against stock - but does not reserve it

`addItem` and `updateItemQuantity` both check the requested quantity
against Phase 6's `Inventory.quantity` and reject with the same
`InsufficientStockException` (409) that Phase 6 introduced for actual
stock reduction. This is a deliberate reuse of that exception - the
error shape a client sees is identical whether the rejection happened
at cart-add time or at real checkout time.

**Important distinction:** this check does **not** call
`InventoryService.reduceStock` - nothing is actually deducted or held
aside for a cart. Stock is only genuinely consumed when an order is
placed (Phase 8), using the same atomic, concurrency-safe
`reduceStockIfAvailable` this phase's check reads from. That means two
customers can both have the last unit of a product sitting in their
carts at the same time; whoever completes checkout first gets it, and
the other will see an insufficient-stock error **at that point**, not
before. This is standard, expected e-commerce behavior - carts are not
inventory reservations.

---

## 5. Cross-user isolation

A cart item's id is just an auto-increment number - nothing stops a
malicious client from guessing `PUT /api/v1/cart/items/17` even if
item 17 belongs to someone else. `CartService` guards every
item-specific mutation (`updateItemQuantity`, `removeItem`) with
`assertItemBelongsToCaller`, which checks the item's actual `cart_id`
against the caller's own cart - not against anything the client
claims. A mismatch throws the same `ForbiddenOperationException` (403)
Phase 4 introduced for store/product ownership violations - same
pattern, applied to a different resource.

---

## 6. Run the Phase 7 tests

```bash
mvn test
```

Two new test classes:

1. **`CartServiceTest`** (Mockito unit tests) - new item creation,
   quantity-increase on re-adding an existing product, unavailable
   product rejection, insufficient-stock rejection, lazy cart
   creation, cross-user update/remove rejection, and a dedicated test
   proving cart totals change when the underlying product's price
   changes (the price-integrity guarantee, verified directly).

2. **`CartControllerIntegrationTest`** (full `@SpringBootTest` +
   `MockMvc`, real Spring Security chain) - the complete flow:
   - No token -> `401`.
   - Brand-new user gets an empty cart, created automatically.
   - Adding an item persists it and returns the backend-computed price.
   - **A request body with a smuggled `"price"`/`"unitPrice"` field is
     completely ignored** - the response still reflects the real
     product price, not the attacker-supplied value.
   - Adding the same product twice increases quantity instead of
     duplicating the row.
   - Over-stock and unavailable-product additions -> `409`.
   - Update, remove, and clear-cart all work correctly.
   - **Cross-user isolation**: customer two cannot update or remove
     customer one's cart item (`403`), and each user's cart is
     independently empty/populated as expected.
   - Setting quantity to `0` via update is rejected by bean validation
     (`400`) - removal is a separate, explicit operation.

These run alongside every earlier phase's suite in the same `mvn test`
invocation, so nothing from Phases 1-6 is skipped as regression coverage.

---

## 7. Common errors & fixes (Phase 7 additions)

| Error | Cause | Fix |
|---|---|---|
| `409` adding a product you're sure has stock | Someone else already added/checked-out the remaining units since you last checked | `GET /api/v1/products/{id}/inventory` to see the current count |
| `403` updating/removing a cart item | The item id belongs to a different user's cart | Only ever act on item ids returned from your own `GET /api/v1/cart` response |
| `400` setting quantity to `0` | `UpdateCartItemRequest` requires `quantity >= 1` | Use `DELETE /api/v1/cart/items/{itemId}` to remove an item instead |
| Cart total looks different than expected | This is by design - prices are always read live from the current `Product` row | Confirm the product's current `price`/`discount` via `GET /api/v1/products/{id}` |

---

## 8. What's next

**Phase 7 is complete. Stopping here, as instructed.**

Phase 8 (per the roadmap) builds Order Management on top of this:
checkout turns a Cart into an `Order` + `OrderItem`s, this time
**snapshotting** price at the moment of purchase (unlike Cart, which
always stays live), calculating subtotal/discount/delivery
fee/final total entirely on the backend, and - critically - this is
where stock is actually, genuinely reduced via Phase 6's
`reduceStockIfAvailable`, with proper order-status transitions
(`CREATED` -> `PAYMENT_PENDING` -> ... -> `DELIVERED`).

Say the word when you're ready to move to **Phase 8**.
