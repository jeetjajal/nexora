# NEXORA — Phase 5: Products & Categories

Continues directly from Phases 1–4. **No working Phase 1–4 code was
overwritten.** Every change in this phase is either a new file, or a
small additive change to an existing file (documented in §1) — nothing
existing was rewritten or removed.

Still no Redis, Kafka, AI, payments, microservices, or Docker.

---

## 1. Step 1 — what was reviewed before writing anything

Per your instruction, work started with a review of the current
project, not new code:

- **Package structure** — confirmed `store`, `category`, `product`,
  `inventory` each already had `entity/repository/dto/mapper` (Phase 2)
  plus `service/controller` (Phase 4). Phase 5 needed to *extend* the
  repository and service layers, not recreate any of it.
- **Database relationships** — confirmed from Phase 2:
  `Store —ManyToOne→ owner`, `Store ←ManyToMany→ Category`
  (`store_categories`), `Product —ManyToOne→ Store`,
  `Product ←ManyToMany→ Category` (`product_categories`),
  `Product ←OneToOne→ Inventory`. These are exactly what the new
  Criteria-API filters below join against.
- **Phase 4 tests** — `StoreProductInventoryIntegrationTest` was read
  in full to confirm none of its assertions would break under the
  Phase 5 changes (see §6 "regression spot-checks", which specifically
  re-verifies its ownership rules still hold).

### What was added or touched, file by file

| File | Change |
|---|---|
| `ProductRepository` | Added `extends JpaSpecificationExecutor<Product>` — additive, no existing method touched |
| `StoreRepository` | Added `extends JpaSpecificationExecutor<Store>` — additive, no existing method touched |
| NEW `product/spec/ProductSpecifications.java` | Composable filter builders |
| NEW `store/spec/StoreSpecifications.java` | Composable filter builders |
| `ProductService` | Added `searchProducts(...)`; `getAllProducts(Pageable)` kept as a backward-compatible wrapper calling it with all filters null; added discount-vs-price validation in `createProduct`/`updateProduct` |
| `StoreService` | Added `searchStores(...)`; `getAllStores(Pageable)` kept as a backward-compatible wrapper |
| `ProductController` | `GET /api/v1/products` gained optional query params (see §3) — same path, same method name, backward compatible |
| `StoreController` | `GET /api/v1/stores` gained optional query params — same path, backward compatible |
| NEW `product/exception/InvalidProductDataException.java` | 400 for discount > price |
| `GlobalExceptionHandler` | Added one new `@ExceptionHandler` for the exception above — additive |

Nothing about Category CRUD, cross-store ownership checks, or the
JWT/security layer changed at all in this phase — Category management
and the two-layer authorization model were already complete from
Phase 4 and needed no new work, only regression verification.

---

## 2. Why `Specification` instead of more derived-query methods

Phase 4's repositories used simple derived queries
(`findByStoreId`, `existsByCategoriesId`, etc.) — one method per fixed
query shape. Search is different: a customer might filter by *any
combination* of name, category, price range, and availability. Writing
a derived method for every combination of 5 optional filters would
mean up to 2^5 = 32 methods. Instead, `ProductSpecifications` and
`StoreSpecifications` each provide small, independent filter builders
that `ProductService`/`StoreService` **compose** at request time:

```java
Specification<Product> spec = Specification
        .where(ProductSpecifications.nameContains(name))
        .and(ProductSpecifications.hasCategoryName(categoryName))
        .and(ProductSpecifications.belongsToStore(storeId))
        .and(ProductSpecifications.priceGreaterThanOrEqual(minPrice))
        .and(ProductSpecifications.priceLessThanOrEqual(maxPrice))
        .and(ProductSpecifications.isAvailable(available));
```

Each builder method returns `null` when its filter wasn't requested;
Spring Data's `Specification.and(null)` is explicitly null-safe and
simply contributes no condition — so a search with only `minPrice` set
produces a genuinely simpler SQL query than one with all six filters
set, not "match everything on the missing ones."

**Sorting** doesn't need any of this — it rides entirely on
`Pageable`'s `Sort` (e.g. `?sort=price,asc`), which Spring Data already
translates into `ORDER BY` automatically. No custom sort code was
needed.

---

## 3. New query parameters

### Products — `GET /api/v1/products`

| Param | Type | Meaning |
|---|---|---|
| `name` | string | case-insensitive substring match |
| `categoryName` | string | exact match (case-insensitive) on an attached category |
| `storeId` | number | restrict to one store |
| `minPrice` / `maxPrice` | decimal | inclusive price range |
| `available` | boolean | filter by availability flag |
| `page`, `size`, `sort` | — | standard Spring Data pagination/sorting (unchanged from Phase 4) |

Example:
```
GET /api/v1/products?name=pizza&categoryName=Pizza&minPrice=100&maxPrice=300&available=true&sort=price,asc&page=0&size=10
```

### Stores — `GET /api/v1/stores`

| Param | Type | Meaning |
|---|---|---|
| `name` | string | case-insensitive substring match |
| `categoryName` | string | exact match on an attached category |
| `status` | `OPEN` \| `CLOSED` \| `SUSPENDED` | filter by store status |
| `page`, `size`, `sort` | — | standard pagination/sorting |

All parameters on both endpoints are optional — calling either with no
query params at all behaves exactly as it did in Phase 4 (list
everything, paginated), which is what the regression tests in §6 verify.

---

## 4. Discount validation (new in Phase 5)

Bean validation (`@DecimalMin` on `ProductRequest`, from Phase 4) only
checks each field on its own — it can't express a rule that relates
*two* fields to each other. Phase 5 adds that check in the service
layer: a product's `discount` can never be greater than its `price`.

```json
{
  "success": false,
  "message": "Discount (150.00) cannot be greater than price (100.00)",
  "timestamp": "2026-08-20T10:00:00"
}
```
(`400 Bad Request`, same status class as a bean-validation failure —
from the client's point of view it's the same kind of problem: "the
data you sent doesn't make sense.")

---

## 5. Authorization — unchanged, confirmed still correct

The Phase 4 two-layer model (role check via `@PreAuthorize` at the
controller, ownership check inside the service) applies unchanged to
every endpoint in this phase:

| Action | ADMIN | STORE_OWNER | CUSTOMER |
|---|---|---|---|
| Search/filter/sort/view products or stores | Yes | Yes | Yes |
| Create/update category | Yes | No | No |
| Create/update product or store | Yes (any) | Yes (own only) | No |
| Cross-store product/store edits | Yes (bypasses ownership check) | No (`403`) | No |

No endpoint, request DTO, or authorization rule from Phase 4 was
loosened or tightened — search/filter/sort is purely a **read**
capability layered on top of already-public `GET` endpoints, and the
one new validation rule (discount <= price) applies equally to ADMIN and
STORE_OWNER, consistent with existing create/update authorization.

---

## 6. Run the Phase 5 tests

```bash
mvn test
```

`SearchFilterSortIntegrationTest` (new, Phase 5) covers:

- **Search** — product name substring (case-insensitive), store name substring.
- **Filter** — product by category, by price range, by availability;
  store by category, by status.
- **Sort** — products by price ascending, via the standard `sort` param.
- **Combined filters** — name + price range + availability together in
  one request.
- **Pagination** — `page`/`size` correctly limit results and report
  `totalElements`/`totalPages`.
- **Validation** — discount greater than price returns `400` (and
  confirms via `ProductRepository` that nothing was persisted); a
  valid discount returns `201`.
- **Regression spot-checks** — cross-store product creation is still
  `403` after all the above changes; unfiltered `GET /api/v1/products`
  still works exactly as in Phase 4; category creation is still
  ADMIN-only.

This runs **alongside**, not instead of, every earlier suite:
`UserServiceTest` (Phase 1/2), `RelationshipRepositoryTest` (Phase 2),
`JwtServiceTest` / `AuthServiceTest` / `AuthControllerIntegrationTest`
(Phase 3), and `StoreProductInventoryIntegrationTest` (Phase 4) all run
in the same `mvn test` invocation — a single green run across all six
test classes is the actual regression signal for Phases 1–5 together.

---

## 7. Final verification — what I can confirm here, and what needs your machine

I don't have network access to Maven Central or a JDK/Maven toolchain
in this sandbox, so I can't literally execute `mvn test` myself (same
limitation noted back in Phase 1). What I did do before delivering
this phase:

- Manually re-read every modified file end-to-end for import
  correctness, method signatures, and consistency with the Phase 2/4
  entities and DTOs it depends on.
- Traced every new endpoint's request through controller, service,
  repository, and entity by hand to confirm field names used in
  `ProductSpecifications`/`StoreSpecifications` (`"name"`, `"price"`,
  `"available"`, `"categories"`, `"store"`, `"status"`) match the
  actual entity field names from Phase 2.
- Confirmed no secrets are hard-coded anywhere new in this phase — no
  new config/credentials were introduced at all; Phase 5 is pure
  application code.

**What you should run locally to close the loop:**

```bash
mvn test               # full suite, all 6 test classes together
mvn spring-boot:run    # smoke-test a couple of the new query params manually
```

### Git — commit and push to your existing history

I don't have access to your local Git repository — each phase has been
delivered to you as a zip, not pushed anywhere — so I can't run
`git status` / `commit` / `push` against your actual `main` branch
myself. Once you've extracted this zip **over your existing Phase 4
working copy** (so Git sees it as a diff, not a fresh project) and
confirmed `mvn test` passes:

```bash
git status                              # review what changed — should match section 1's table
git add .
git commit -m "Phase 5: product/category search, filter, sort, pagination, discount validation"
git push origin main
```

If you'd rather I prepare the changed files as an actual git diff/patch
against your Phase 4 commit instead of a full zip, let me know and I
can generate that instead.

---

## 8. What's next

**Phase 5 is complete. Stopping here, as instructed.**

Phase 6 (per the roadmap) moves to concurrency-safe **Inventory**
operations — dedicated increase/reduce endpoints with proper
transactional guarantees, replacing the "set to absolute value"
operation built in Phase 4, and explicitly setting up the
stock-race-condition problem that Phase 21 later solves with
optimistic/pessimistic locking.

Say the word when you're ready to move to **Phase 6**.
