# NEXORA — Phase 1: Spring Boot Foundation

**Intelligent Food, Grocery & Local-Commerce Platform**
Theme: **Modern Commerce** — fresh green (#2E7D32-ish), white/off-white background, dark charcoal text,
soft gray sections, rounded cards. (Applied to the **frontend** once we build UI in a later phase —
Phase 1 is backend-only, per the roadmap.)

This phase builds the foundation only: Spring Boot + MySQL + a working user registration API.
No Redis, Kafka, AI, payments, Docker, AWS, Kubernetes, or microservices yet — that's intentional.

---

## 1. What got built

```
nexora/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/nexora/
    │   │   ├── NexoraApplication.java        ← app entry point
    │   │   ├── common/
    │   │   │   └── ApiResponse.java          ← standard response envelope
    │   │   ├── exception/
    │   │   │   ├── DuplicateEmailException.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   └── user/
    │   │       ├── entity/User.java          ← maps to `users` table
    │   │       ├── repository/UserRepository.java
    │   │       ├── dto/RegisterRequest.java
    │   │       ├── dto/UserResponse.java
    │   │       ├── mapper/UserMapper.java
    │   │       ├── service/UserService.java  ← business logic
    │   │       └── controller/UserController.java
    │   └── resources/
    │       └── application.properties        ← MySQL + JPA config
    └── test/
        └── java/com/nexora/user/
            └── UserServiceTest.java          ← JUnit 5 + Mockito tests
```

This follows **Controller → Service → Repository → MySQL**, the layering the whole project
will keep using as it grows.

- **Controller**: only handles HTTP in/out. No business rules here.
- **Service**: all the actual logic (duplicate-email check, password hashing).
- **Repository**: talks to MySQL via Spring Data JPA — no manual SQL needed for basic CRUD.
- **Entity**: the Java class that JPA/Hibernate maps 1:1 to a MySQL table.
- **DTO**: the shape of data sent to/from the API — deliberately separate from the Entity so
  we never leak internal fields (like the password hash) to clients.

---

## 2. MySQL setup

1. Make sure MySQL 8+ is installed and running locally.
2. You don't need to manually create tables — `spring.jpa.hibernate.ddl-auto=update` in
   `application.properties` tells Hibernate to auto-create the `users` table from the `User`
   entity the first time the app starts. You only need the database itself to exist:

```sql
CREATE DATABASE IF NOT EXISTS nexora_db;
```

(Also handled automatically since the connection URL includes
`createDatabaseIfNotExist=true`, but running it yourself is fine too.)

3. Open `src/main/resources/application.properties` and set your real MySQL username/password:

```properties
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

---

## 3. Run the project

```bash
cd nexora
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

On startup, check the logs — you should see Hibernate creating the `users` table
(`create table users (...)`) if it doesn't already exist.

---

## 4. API — Register a user

**POST** `http://localhost:8080/api/v1/auth/register`

Request body:
```json
{
  "name": "Aditi Sharma",
  "email": "aditi@example.com",
  "password": "SecurePass123",
  "phone": "9876543210"
}
```

Success response — `201 Created`:
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 1,
    "name": "Aditi Sharma",
    "email": "aditi@example.com",
    "phone": "9876543210",
    "status": "ACTIVE",
    "role": "CUSTOMER",
    "createdAt": "2026-08-15T10:15:30"
  },
  "timestamp": "2026-08-15T10:15:30"
}
```

Duplicate email — `409 Conflict`:
```json
{
  "success": false,
  "message": "Email already registered: aditi@example.com",
  "timestamp": "2026-08-15T10:15:31"
}
```

Validation failure (e.g. short password, bad email) — `400 Bad Request`:
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "email": "Email must be a valid email address",
    "password": "Password must be at least 8 characters long"
  },
  "timestamp": "2026-08-15T10:15:32"
}
```

**GET** `http://localhost:8080/api/v1/auth/{id}` → fetch a user by ID (handy for manually
confirming a row landed in MySQL). Full authenticated "my profile" endpoints arrive in Phase 3.

---

## 5. Run the tests

```bash
mvn test
```

`UserServiceTest` checks, without touching a real database:
- Registration succeeds and returns a proper `UserResponse` when the email is free.
- Registering with a taken email throws `DuplicateEmailException` and `save()` is never called.
- The password stored is never the raw, plain-text password (it's BCrypt-hashed).

---

## 6. Common errors & fixes

| Error | Cause | Fix |
|---|---|---|
| `Communications link failure` | MySQL isn't running, or wrong host/port | Start MySQL; confirm it's on `localhost:3306` |
| `Access denied for user 'root'@'localhost'` | Wrong password in `application.properties` | Update `spring.datasource.password` |
| `Unknown database 'nexora_db'` | Database not created and auto-create didn't fire | Run `CREATE DATABASE nexora_db;` manually |
| `Table 'nexora_db.users' doesn't exist` | `ddl-auto` misconfigured or app never started cleanly | Confirm `spring.jpa.hibernate.ddl-auto=update` and check startup logs for errors |
| `400 Bad Request` on register | A field failed validation (see `data` in the response) | Check the field-level messages returned |
| Port 8080 already in use | Another process is using it | Stop it, or add `server.port=8081` |

---

## 7. Why these choices (for interview prep)

- **DTOs instead of exposing the entity directly** — protects internal schema, hides the
  password hash, decouples API contract from database structure.
- **BCrypt for password hashing** — one-way hash; even we can't recover the original password,
  and it's intentionally slow to resist brute-force attacks.
- **Global exception handler (`@RestControllerAdvice`)** — one place for consistent error
  JSON and HTTP status codes, instead of try/catch scattered across controllers.
- **Standard `ApiResponse<T>` envelope** — every endpoint (now and in later phases) returns the
  same predictable shape, which simplifies frontend integration.
- **`ddl-auto=update` for now** — fine for early development; production systems typically
  switch to `validate` and manage schema changes with Flyway/Liquibase migrations (a later phase).

---

## 8. What's next

**Phase 1 is complete. Stopping here, as instructed.**

Phase 2 will expand the data model — `Role`, `Address`, `Store`, `Category`, `Product`,
`Inventory` entities with proper relationships (`@OneToMany`, `@ManyToOne`, `@ManyToMany`),
keys, constraints, and indexes — still MySQL only, no Redis/Kafka/AI/payments/microservices.

Say the word when you're ready to move to **Phase 2**.
