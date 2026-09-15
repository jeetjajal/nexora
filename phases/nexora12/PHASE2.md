# NEXORA — Phase 2: Database & JPA

Continues directly from Phase 1. **Phase 1 files are untouched**, except for
the one change Phase 2 itself requires: `User` gained proper `roles` and
`addresses` relationships (see section 5 below) instead of the flat
`role` string column from Phase 1.

No Redis, Kafka, AI, payment, or microservices — still backend + MySQL only.

---

## 1. What got built

New entities, each with its own repository and response DTO:

```
src/main/java/com/nexora/
├── role/
│   ├── entity/Role.java              ← id, name (enum: CUSTOMER, STORE_OWNER, DELIVERY_PARTNER, ADMIN)
│   ├── entity/RoleName.java
│   └── repository/RoleRepository.java
├── address/
│   ├── entity/Address.java           ← belongs to one User
│   ├── repository/AddressRepository.java
│   ├── dto/AddressResponse.java
│   └── mapper/AddressMapper.java
├── store/
│   ├── entity/Store.java             ← owned by one User (STORE_OWNER), has many Categories
│   ├── entity/StoreStatus.java
│   ├── repository/StoreRepository.java
│   ├── dto/StoreResponse.java
│   └── mapper/StoreMapper.java
├── category/
│   ├── entity/Category.java          ← shared vocabulary for stores AND products
│   ├── repository/CategoryRepository.java
│   ├── dto/CategoryResponse.java
│   └── mapper/CategoryMapper.java
├── product/
│   ├── entity/Product.java           ← belongs to one Store, has many Categories
│   ├── repository/ProductRepository.java
│   ├── dto/ProductResponse.java
│   └── mapper/ProductMapper.java
├── inventory/
│   ├── entity/Inventory.java         ← exactly one per Product
│   ├── repository/InventoryRepository.java
│   └── dto/InventoryResponse.java
└── config/
    └── DataSeeder.java               ← seeds the 4 fixed roles on startup
```

Plus: `src/test/java/com/nexora/phase2/RelationshipRepositoryTest.java` —
real database tests (H2 in-memory) proving every relationship actually
works, and `src/test/resources/application.properties` so tests never
need MySQL running.

No new controllers yet — Phase 2 is data-model only. Store/Product/Category
CRUD APIs arrive in Phases 4–5, once Spring Security (Phase 3) exists to
authorize "only the store owner can edit their own store."

---

## 2. Entity-relationship summary

```
User (1) ────< Address (many)              [@OneToMany / @ManyToOne]
User (many) >──< Role (many)               [@ManyToMany via user_roles]
User (1) ────< Store (many, as owner)      [@OneToMany / @ManyToOne]
Store (many) >──< Category (many)          [@ManyToMany via store_categories]
Store (1) ────< Product (many)             [@OneToMany / @ManyToOne]
Product (many) >──< Category (many)        [@ManyToMany via product_categories]
Product (1) ──── Inventory (1)             [@OneToOne]
```

MySQL tables Hibernate will create/manage from these entities:
`users`, `roles`, `user_roles`, `addresses`, `stores`, `store_categories`,
`categories`, `products`, `product_categories`, `inventory`.

Keys, constraints, indexes used:
- Every entity: auto-increment `IDENTITY` primary key.
- `users.email`, `categories.name`, `roles.name` → unique constraints.
- `inventory.product_id` → unique (this is what actually enforces the
  one-to-one; without `unique = true` it would just be a one-to-many).
- Indexes added on foreign-key columns that will be queried often:
  `addresses.user_id`, `stores.owner_id`, `products.store_id`.
- `@ManyToMany` join tables (`user_roles`, `store_categories`,
  `product_categories`) get composite keys on their two foreign columns,
  managed entirely by Hibernate.

---

## 3. Lazy vs Eager loading — explained simply

When Hibernate loads an entity from the database, a relationship field
(like `store.getCategories()`) can be fetched in one of two ways:

- **EAGER** — load it immediately, every time, as part of the same query
  (or a follow-up query fired automatically). Simple, but wasteful if you
  don't actually need that related data most of the time.
- **LAZY** — don't load it yet. Only run the extra query the moment code
  actually calls the getter (e.g. `store.getCategories()`). If nobody
  calls it, that query never happens.

**Where we used which, and why:**

| Relationship | Fetch type | Reasoning |
|---|---|---|
| `User.roles` | **EAGER** | Only ever 1–4 tiny rows; needed on almost every authenticated request in Phase 3 for authorization checks. Loading it always is cheaper than firing an extra lazy query almost every time anyway. |
| `User.addresses` | LAZY | Most of the time we load a User we don't need their full address list (e.g. just checking their name/email). Only checkout screens need it. |
| `Store.owner` | LAZY | A product list screen showing 20 stores shouldn't pull in 20 full owner User objects unless the UI actually displays owner details. |
| `Store.categories` / `Product.categories` | LAZY | Browsing products doesn't always need category names loaded — and `@ManyToMany` in particular can get expensive to eager-load across many rows. |
| `Product.store` | LAZY | Same reasoning as `Store.owner`. |
| `Inventory.product` | LAZY | Inventory checks (e.g. "is this in stock?") don't need the full Product object. |

**The catch with LAZY:** if you try to access a lazy field *after* the
database session/transaction has already closed, you get a
`LazyInitializationException`. This is why, once we build controllers on
top of these entities (Phase 4 onward), we'll map Entity → DTO (like
`StoreMapper.toResponse()`) **inside** the `@Transactional` service
method, while the session is still open — never leak raw entities out to
the controller layer and try to touch lazy fields later.

---

## 4. Role seeding

`config/DataSeeder.java` runs once on every app startup and makes sure
the 4 fixed roles (`CUSTOMER`, `STORE_OWNER`, `DELIVERY_PARTNER`, `ADMIN`)
exist as rows in the `roles` table — inserting any that are missing,
skipping any that already exist. This has to happen before any user can
be assigned a role, since `user_roles` rows reference real `role_id`
values via a foreign key.

---

## 5. What changed in `User` (Phase 1 → Phase 2)

Phase 1's `User` had a single `role` string column. Phase 2's task list
explicitly says *"Create User, Role, Address... entities with correct
relationships"* — so `User` itself had to gain the relationship fields.
Nothing about registration's external behavior changed:

- `POST /api/v1/auth/register` still works exactly the same from the
  client's point of view — it just now assigns a real `Role` entity
  (`CUSTOMER`) instead of writing the string `"CUSTOMER"`.
- `UserResponse.role` (a single string) became `UserResponse.roles` (a
  set of strings) to reflect that a user can now hold more than one role.

---

## 6. Run the Phase 2 tests

```bash
mvn test
```

Two test classes now run:

1. **`UserServiceTest`** (Phase 1, updated) — still uses Mockito, still
   proves registration logic works in isolation, now also verifying the
   `CUSTOMER` role is correctly looked up and assigned.

2. **`RelationshipRepositoryTest`** (new, Phase 2) — uses `@DataJpaTest`
   against a real in-memory H2 database (auto-configured, no setup
   needed) to prove, with actual saved-and-reloaded rows, that:
   - A user can hold multiple roles (`user_roles` join table works).
   - A user can have multiple addresses, each correctly linked back.
   - A store has exactly one owner and can have multiple categories.
   - A product belongs to one store and can have multiple categories.
   - A product has exactly one inventory record.
   - Category names are enforced unique.

This is what "test database operations" means in this phase — not
mocking, but actually exercising MySQL-equivalent relational behavior
via H2 so the whole suite runs fast and needs no external database.

---

## 7. Start the app (still needs real MySQL)

Nothing changed in how you run the actual application — same as Phase 1:

```bash
mvn spring-boot:run
```

On startup you'll now see Hibernate create several new tables
(`roles`, `user_roles`, `addresses`, `stores`, `store_categories`,
`categories`, `products`, `product_categories`, `inventory`) in addition
to `users`, and `DataSeeder` will insert the 4 role rows.

---

## 8. Common errors & fixes (Phase 2 additions)

| Error | Cause | Fix |
|---|---|---|
| `Table 'user_roles' doesn't exist` | App wasn't restarted after pulling Phase 2 code | Restart `mvn spring-boot:run` so Hibernate re-runs `ddl-auto=update` |
| `CUSTOMER role not found` (IllegalStateException) on register | `DataSeeder` didn't run yet / roles table empty | Restart the app cleanly; check startup logs for `DataSeeder` running without errors |
| `LazyInitializationException` (if you add your own code later) | Trying to access a LAZY field (e.g. `store.getOwner().getName()`) outside a transaction | Access lazy fields inside the `@Transactional` service method, or map to a DTO before returning |
| H2 test failures about dialect/mode | Test module out of sync | Confirm `com.h2database:h2` is in `pom.xml` under `test` scope |

---

## 9. What's next

**Phase 2 is complete. Stopping here, as instructed.**

Phase 3 will add real authentication: Spring Security, BCrypt (already
wired in from Phase 1), JWT issuing/validation, `AuthController`,
`AuthService`, and role-based authorization for all four roles — plus
tests for duplicate email, successful login, and wrong password.

Say the word when you're ready to move to **Phase 3**.
