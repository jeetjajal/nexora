# NEXORA — Phase 4: Store & Product Management

Continues directly from Phases 1–3. **The Phase 3 security layer was
extended, not rebuilt**: `SecurityConfig` gained one additive annotation
(`@EnableMethodSecurity`, to unlock `@PreAuthorize`) — the JWT filter,
`AuthController`, login flow, and stateless session config are all
untouched.

Per your instruction, this phase started by **inspecting what already
existed** from Phase 2 (`Store`, `Category`, `Product`, `Inventory`
entities, repositories, response DTOs, mappers — all already correct
and reused as-is) before writing anything new. Phase 4 adds exactly the
missing layer: **request DTOs, services (with authorization), and
controllers.**

Still no Redis, Kafka, AI, payments, microservices, or Docker.

---

## 1. What already existed (Phase 2) vs. what Phase 4 added

| Layer | Store | Category | Product | Inventory |
|---|---|---|---|---|
| Entity | ✅ Phase 2 | ✅ Phase 2 | ✅ Phase 2 | ✅ Phase 2 |
| Repository | ✅ Phase 2 (+2 methods added) | ✅ Phase 2 | ✅ Phase 2 (+2 methods added) | ✅ Phase 2 |
| Response DTO + Mapper | ✅ Phase 2 | ✅ Phase 2 | ✅ Phase 2 | ✅ Phase 2 |
| **Request DTO** | 🆕 `StoreRequest`, `StoreStatusRequest` | 🆕 `CategoryRequest` | 🆕 `ProductRequest` | 🆕 `InventoryUpdateRequest` |
| **Service (business logic + authorization)** | 🆕 `StoreService` | 🆕 `CategoryService` | 🆕 `ProductService` | 🆕 `InventoryService` |
| **Controller** | 🆕 `StoreController` | 🆕 `CategoryController` | 🆕 `ProductController` | 🆕 `InventoryController` |

Two small additions to existing Phase 2 repositories, both purely
additive (no existing method changed):
- `StoreRepository.existsByCategoriesId(Long)` and
  `ProductRepository.existsByCategoriesId(Long)` — used by
  `CategoryService` to safely block deleting a category still in use
  (see §5).
- `ProductRepository.findByStoreId(Long, Pageable)` — a paginated
  overload alongside the existing `List<Product> findByStoreId(Long)`.

Two new shared exceptions: `ForbiddenOperationException` (403, for
ownership violations) and `CategoryInUseException` /
`DuplicateCategoryException` (409), all wired into the existing
`GlobalExceptionHandler` from Phase 1.

---

## 2. The authorization model — two layers, working together

```
Request
  │
  ▼
JwtAuthenticationFilter (Phase 3, unchanged)
  → is there a valid token at all? who is the caller?
  │
  ▼
@PreAuthorize on the controller method (Phase 4, new)
  → does the caller's ROLE allow this endpoint AT ALL?
    e.g. "hasAnyRole('STORE_OWNER','ADMIN')"
  → a CUSTOMER is rejected here with 403, before any business logic runs
  │
  ▼
Service-layer ownership check (Phase 4, new)
  → does the caller own THIS SPECIFIC resource?
    e.g. store.getOwner().getId().equals(caller.getId())
  → ADMIN always bypasses this check
  → a STORE_OWNER who owns OTHER stores, but not this one, is
    rejected here with 403 (ForbiddenOperationException)
```

**Why two layers instead of one?** `@PreAuthorize` only knows about
roles — it has no idea which store id is in the URL or who owns it.
Role checks and resource-ownership checks are genuinely different
questions, so they live in different places: role gates at the
controller (cheap, fails fast, no DB call needed), ownership checks in
the service (after the actual row has been loaded from MySQL).

### Role rules, endpoint by endpoint

| Resource | Create | Read | Update | Delete/Status |
|---|---|---|---|---|
| **Store** | STORE_OWNER, ADMIN | any authenticated role | owner or ADMIN | owner or ADMIN |
| **Category** | ADMIN only | any authenticated role | ADMIN only | ADMIN only |
| **Product** | owner of the store, or ADMIN | any authenticated role | owner or ADMIN | owner or ADMIN |
| **Inventory** | (created automatically with product) | any authenticated role | owner or ADMIN | — |

---

## 3. Endpoints

### Store

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/stores` | STORE_OWNER, ADMIN |
| GET | `/api/v1/stores?page=0&size=10` | any authenticated |
| GET | `/api/v1/stores/{id}` | any authenticated |
| GET | `/api/v1/stores/my` | STORE_OWNER, ADMIN — caller's own stores |
| PUT | `/api/v1/stores/{id}` | owner or ADMIN |
| PATCH | `/api/v1/stores/{id}/status` | owner or ADMIN |

### Category

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/categories` | ADMIN |
| GET | `/api/v1/categories` | any authenticated |
| GET | `/api/v1/categories/{id}` | any authenticated |
| PUT | `/api/v1/categories/{id}` | ADMIN |
| DELETE | `/api/v1/categories/{id}` | ADMIN |

### Product

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/stores/{storeId}/products` | owner of `storeId`, or ADMIN |
| GET | `/api/v1/stores/{storeId}/products?page=0&size=10` | any authenticated |
| GET | `/api/v1/products?page=0&size=10` | any authenticated |
| GET | `/api/v1/products/{id}` | any authenticated |
| PUT | `/api/v1/products/{id}` | owner or ADMIN |
| PATCH | `/api/v1/products/{id}/deactivate` | owner or ADMIN (soft delete: `available=false`) |
| DELETE | `/api/v1/products/{id}` | owner or ADMIN (hard delete) |

### Inventory

| Method | Path | Access |
|---|---|---|
| GET | `/api/v1/products/{productId}/inventory` | any authenticated |
| PUT | `/api/v1/products/{productId}/inventory` | owner of the product's store, or ADMIN |

---

## 4. Sample requests

**Create a store** (STORE_OWNER token required):
```
POST /api/v1/stores
Authorization: Bearer <token>

{
  "name": "Rajkot Pizza Hub",
  "description": "Wood-fired pizza, fast delivery.",
  "imageUrl": "https://example.com/store.jpg",
  "openingTime": "10:00:00",
  "closingTime": "23:00:00",
  "categoryNames": ["Pizza", "Fast Food"]
}
```
`categoryNames` must already exist (an ADMIN creates them first via
`POST /api/v1/categories`) — a store owner attaches existing
categories, but can't invent new ones inline.

**Create a product under that store**, seeding its starting stock:
```
POST /api/v1/stores/{storeId}/products
Authorization: Bearer <token>

{
  "name": "Margherita Pizza",
  "description": "Fresh mozzarella, tomato sauce and herbs.",
  "price": 249.00,
  "discount": 10.00,
  "categoryNames": ["Pizza"],
  "initialStock": 20
}
```
This creates the `Product` row **and** its paired `Inventory` row in
the same transaction (Phase 2's one-to-one relationship requires every
product to have exactly one inventory record).

**Update stock later:**
```
PUT /api/v1/products/{productId}/inventory
Authorization: Bearer <token>

{ "quantity": 35 }
```

**Attempting to edit someone else's store:**
```json
{
  "success": false,
  "message": "You do not have permission to modify a store you do not own.",
  "timestamp": "2026-08-19T10:00:00"
}
```
(`403 Forbidden`, thrown by `StoreService`, not by Spring Security
itself — the caller genuinely had the STORE_OWNER role, so
`@PreAuthorize` let them through; the *specific store* is what they're
not allowed to touch.)

---

## 5. Deleting a category safely

`Category` is referenced from the *owning* side of the relationship —
`Store` and `Product` each declare the `@JoinTable`
(`store_categories`, `product_categories`) that points at
`categories.id` (Phase 2). Naively deleting an in-use `Category` row
would fail at the database level with a raw foreign-key violation
(`DataIntegrityViolationException` → an ugly `500`). `CategoryService`
checks first, via `StoreRepository.existsByCategoriesId(id)` and
`ProductRepository.existsByCategoriesId(id)`, and returns a clean `409
Conflict` (`CategoryInUseException`) instead — telling the ADMIN
exactly why the delete can't proceed.

---

## 6. Pagination

`GET /api/v1/stores`, `GET /api/v1/products`, and
`GET /api/v1/stores/{storeId}/products` all support standard Spring
Data pagination query parameters, auto-bound by Spring Boot (no extra
dependency needed — it's already wired in via `spring-boot-starter-data-jpa`
+ `spring-boot-starter-web`):

```
GET /api/v1/stores?page=0&size=10&sort=name,asc
```

Response shape wraps the standard `Page<T>` JSON inside our
`ApiResponse` envelope: `data.content` (the array), plus
`data.totalElements`, `data.totalPages`, `data.number`, etc.

---

## 7. Run the Phase 4 tests

```bash
mvn test
```

`StoreProductInventoryIntegrationTest` (new, Phase 4) — full
`@SpringBootTest` + `MockMvc`, running through the **real** Spring
Security filter chain (JWT filter, `@PreAuthorize`, exception handling)
against H2, covering every item from your testing checklist:

- **Admin access** — ADMIN can create categories, update any store
  regardless of ownership.
- **Store owner access** — a STORE_OWNER can manage their own
  store/products/inventory.
- **Customer restrictions** — CUSTOMER is rejected (`403`) from every
  mutating endpoint (create category, create store, create product).
- **Cross-owner ownership violations** — `ownerTwo` is rejected
  (`403`) trying to edit `ownerOne`'s store, create a product under
  `ownerOne`'s store, or update stock for `ownerOne`'s product — with
  a follow-up DB read proving nothing actually changed.
- **Invalid JWT** — a garbage token on a protected endpoint → `401`.
- **Missing JWT** — no `Authorization` header on a protected endpoint
  → `401`.
- **Full CRUD** — create/read/update/deactivate across
  store/category/product, plus inventory read/update.
- **Database verification** — every mutating test re-queries the
  actual repository afterward (not just the HTTP response) to confirm
  what's really in the database — e.g. confirming a product's paired
  `Inventory` row was created with the right starting quantity, or
  that a rejected update left the store's name unchanged in MySQL/H2.
- **Validation** — a negative stock quantity is rejected with `400`
  before it ever reaches the database.
- **Duplicate / in-use category** — duplicate category name → `409`;
  deleting a category still attached to a store → `409`.

Since there's still no "create a STORE_OWNER/ADMIN account" API (that's
a later admin-management phase), the test seeds users with the roles it
needs directly via `UserRepository`/`RoleRepository`, then mints a real
JWT for them via `JwtService` — exactly the token shape a real login
would produce. Only *how the test user obtained their token* is
short-circuited; the actual request-time filter chain, `@PreAuthorize`,
and ownership checks are all exercised for real.

---

## 8. Common errors & fixes (Phase 4 additions)

| Error | Cause | Fix |
|---|---|---|
| `403` creating a store even with a STORE_OWNER account | Token was issued *before* the role was added to that user | Log in again — roles are embedded in the JWT at issue time (Phase 3) |
| `404 Category not found: X (an ADMIN must create it first)` | `categoryNames` in a store/product request references a name that doesn't exist yet | Have an ADMIN `POST /api/v1/categories` first, or omit `categoryNames` |
| `409` deleting a category | It's still attached to a store or product | Detach it from those stores/products first, or leave it in place |
| `403` from a STORE_OWNER editing what looks like "their" store | The store's `owner_id` doesn't actually match their user id | Confirm via `GET /api/v1/stores/my` which stores they actually own |
| `400` with a `quantity` field error on inventory update | Tried to set stock to a negative number | Inventory can never go below 0 — send a value `>= 0` |

---

## 9. What's next

**Phase 4 is complete. Stopping here, as instructed.**

Phase 5 (per the roadmap) focuses more deeply on Products & Categories
— richer search/filter/sort for customers, pagination refinements, and
keeping cross-store editing prevention airtight; Phase 6 then builds
proper concurrency-safe inventory operations (increase/reduce with
transactional guarantees) on top of the basic stock-setting endpoint
built here.

Say the word when you're ready to move to **Phase 5**.
