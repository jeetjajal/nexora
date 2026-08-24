# NEXORA — Phase 3: Authentication & JWT

Continues directly from Phase 1 + 2. **No Phase 1/2 files were removed or
rewritten** — this phase only adds a new `auth` package and updates the
few integration points (`SecurityConfig` protecting endpoints,
`GlobalExceptionHandler` gaining two new handlers, `pom.xml`/
`application.properties` gaining new dependencies/settings).

Still no Redis, Kafka, AI, payments, or microservices/Docker.

---

## 1. What got built

```
src/main/java/com/nexora/auth/
├── controller/
│   └── AuthController.java        ← POST /login, GET /me (protected)
├── service/
│   └── AuthService.java           ← login logic, issues JWTs
├── dto/
│   ├── LoginRequest.java
│   └── AuthResponse.java
├── exception/
│   ├── InvalidCredentialsException.java   → 401
│   └── AccountNotActiveException.java     → 403
└── security/
    ├── UserPrincipal.java              ← adapts User entity to Spring Security's UserDetails
    ├── NexoraUserDetailsService.java   ← loads a User from MySQL by email
    ├── JwtService.java                 ← generate / parse / validate JWTs
    ├── JwtAuthenticationFilter.java    ← runs once per request, checks Bearer token
    ├── SecurityConfig.java             ← the central SecurityFilterChain
    ├── JwtAuthenticationEntryPoint.java ← consistent 401 JSON body
    └── JwtAccessDeniedHandler.java      ← consistent 403 JSON body
```

Registration (`POST /api/v1/auth/register`) is unchanged and still lives
in `UserController`/`UserService` from Phase 1/2 — Phase 3 only adds
**login** and the JWT machinery around it, matching the architecture
diagram you gave.

---

## 2. Request flow, matching the architecture diagram

```
Client
  │  POST /api/v1/auth/login  { email, password }
  ▼
AuthController
  │
  ▼
AuthService.login()
  │
  ▼
AuthenticationManager.authenticate(email, password)
  │
  ├──► DaoAuthenticationProvider
  │        │
  │        ├──► NexoraUserDetailsService.loadUserByUsername(email) ──► UserRepository ──► MySQL
  │        │
  │        └──► PasswordEncoder.matches(rawPassword, storedHash)   (BCrypt)
  │
  ▼
JwtService.generateToken(principal)
  │
  ▼
Signed JWT returned to client as AuthResponse.accessToken
```

Then, on every later request to a **protected** endpoint:

```
Client
  │  GET /api/v1/auth/me
  │  Authorization: Bearer <token>
  ▼
JwtAuthenticationFilter (runs once per request, before the controller)
  │
  ├─ extract email from token
  ├─ NexoraUserDetailsService.loadUserByUsername(email) ──► MySQL
  ├─ JwtService.isTokenValid(token, principal)?
  │      → checks signature, expiry, email match, account enabled/not locked
  │
  └─ if valid: SecurityContextHolder now holds an authenticated principal
              → request proceeds to AuthController.me()
     if invalid/missing: SecurityContext stays empty
              → SecurityConfig's rule ("anyRequest().authenticated()")
                rejects it → JwtAuthenticationEntryPoint returns 401
```

---

## 3. Endpoints

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Create a new CUSTOMER account (Phase 1/2) |
| POST | `/api/v1/auth/login` | Public | Exchange email+password for a JWT |
| GET | `/api/v1/auth/me` | **Protected** | Return the authenticated user, read from the JWT |
| GET | `/api/v1/auth/{id}` | **Protected** (was public in Phase 1) | Fetch a user by id — now requires a valid token, demonstrating the same rule applies to existing endpoints too |

### Sample: login

**POST** `/api/v1/auth/login`
```json
{
  "email": "aditi@example.com",
  "password": "SecurePass123"
}
```

Success — `200 OK`:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZGl0aUBleGFtcGxlLmNvbSIs...",
    "tokenType": "Bearer",
    "expiresInMs": 3600000,
    "userId": 1,
    "name": "Aditi Sharma",
    "email": "aditi@example.com",
    "roles": ["CUSTOMER"]
  },
  "timestamp": "2026-08-18T10:00:00"
}
```

Wrong password OR unknown email — `401 Unauthorized` (deliberately the
**same** message either way — see §5):
```json
{
  "success": false,
  "message": "Invalid email or password",
  "timestamp": "2026-08-18T10:00:01"
}
```

Correct credentials but account `SUSPENDED` — `403 Forbidden`:
```json
{
  "success": false,
  "message": "Account is not active (current status: SUSPENDED)",
  "timestamp": "2026-08-18T10:00:02"
}
```

### Sample: calling a protected endpoint

**GET** `/api/v1/auth/me`
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

No token / invalid token — `401 Unauthorized`:
```json
{
  "success": false,
  "message": "Authentication required. Provide a valid Bearer token in the Authorization header.",
  "timestamp": "2026-08-18T10:00:03"
}
```

---

## 4. What's in the JWT

The token's payload (readable — NOT encrypted, just signed) contains:

```json
{
  "sub": "aditi@example.com",
  "userId": 1,
  "roles": ["CUSTOMER"],
  "iat": 1755500000,
  "exp": 1755503600
}
```

This is what lets `JwtAuthenticationFilter` authenticate a request
**without a database round trip for authorization data** — the roles
are already right there in the token. We still do one database lookup
per request (`NexoraUserDetailsService.loadUserByUsername`) to catch
accounts that were suspended *after* the token was issued — a small,
deliberate trade-off between pure statelessness and being able to
revoke access from an active session.

---

## 5. Why login always returns the same error for "wrong password" and "unknown email"

If we returned different messages — `"No account with that email"` vs
`"Incorrect password"` — an attacker could use the login endpoint to
enumerate which emails are registered on Nexora at all, one guess at a
time. Returning the identical `"Invalid email or password"` message
(and identical `401` status) for both cases closes that off. This is
standard practice and is exactly what Spring Security's
`DaoAuthenticationProvider` does by default (`BadCredentialsException`
either way) — `AuthService` just passes that behavior through.

---

## 6. Why passwords are never compared "by hand"

`AuthService` never does anything like
`if (password.equals(user.getPassword()))`. Instead, it hands the raw
password to `AuthenticationManager.authenticate(...)`, which internally
calls `PasswordEncoder.matches(rawPassword, storedHash)`. BCrypt hashes
include a random "salt" baked into the hash itself, so the same
password hashed twice produces two *different* stored values — a naive
string comparison would never work correctly here even if we wanted to.

---

## 7. Stateless sessions — what that actually means

`SecurityConfig` sets `SessionCreationPolicy.STATELESS`. This means:
Spring Security will never create an `HttpSession` or issue a session
cookie. Every single request — including two requests half a second
apart from the same logged-in user — is independently authenticated
from scratch, purely from whatever JWT is attached to it. Nothing about
"being logged in" is remembered on the server between requests. This is
what makes it trivial to later run multiple Nexora backend instances
behind a load balancer (Phase 24+) — any instance holding the same JWT
secret can validate any token, with zero shared session state.

---

## 8. CSRF — why it's disabled here

CSRF (Cross-Site Request Forgery) protection exists to stop a malicious
site from making a browser silently replay the *cookies* it already has
for another site. Nexora doesn't use cookies for authentication — the
client must deliberately read the JWT and deliberately attach it as an
`Authorization: Bearer <token>` header on every request. A malicious
site can't make a victim's browser do that automatically, so the attack
CSRF protection defends against doesn't apply the same way to a
stateless, header-based JWT API.

---

## 9. Run the Phase 3 tests

```bash
mvn test
```

Three new test classes, alongside the existing Phase 1/2 ones:

1. **`JwtServiceTest`** — pure unit tests (no Spring context): token
   structure, extracting email/userId/roles, valid-token acceptance,
   rejecting a token issued to a different user, rejecting an expired
   token, rejecting a token signed with a different secret.

2. **`AuthServiceTest`** — Mockito unit tests for `AuthService.login()`:
   successful login, wrong password, unknown email (same generic
   error), suspended account.

3. **`AuthControllerIntegrationTest`** — full `@SpringBootTest` +
   `MockMvc`, running the **real** Spring Security filter chain against
   H2:
   - register → login → get a real, usable JWT
   - duplicate email registration → `409`
   - wrong password login → `401`
   - unknown email login → `401`
   - protected endpoint without a token → `401`
   - protected endpoint with a valid token → `200`, correct user
   - protected endpoint with a garbage token → `401`

---

## 10. Run the app

Same as before:

```bash
mvn spring-boot:run
```

Before running against real MySQL, set a real JWT secret as an
environment variable rather than relying on the development default in
`application.properties`:

```bash
# macOS/Linux
export NEXORA_JWT_SECRET=$(openssl rand -base64 64)

# Windows PowerShell
$env:NEXORA_JWT_SECRET = [Convert]::ToBase64String((1..64 | %{Get-Random -Max 256}))
```

---

## 11. Common errors & fixes (Phase 3 additions)

| Error | Cause | Fix |
|---|---|---|
| `401` on every request, even `/register` | `SecurityConfig`'s `permitAll()` paths don't match your actual request path | Confirm you're calling exactly `/api/v1/auth/register` / `/api/v1/auth/login` |
| `403` immediately, blank body, on a POST | CSRF still enabled somehow | Confirm `.csrf(AbstractHttpConfigurer::disable)` is present in `SecurityConfig` |
| `io.jsonwebtoken.security.WeakKeyException` on startup | `nexora.jwt.secret` too short for the signing algorithm | Use a longer secret (64+ characters); see the `openssl rand -base64 64` command above |
| Login always returns `401` even with the right password | Registered before Phase 3's `DataSeeder`/role wiring changed, or testing against stale data | Re-register the test user against the current Phase 2/3 code |
| `LazyInitializationException` touching `user.getRoles()` far from where it was loaded | Roles are `EAGER` (see Phase 2 docs) so this shouldn't happen for roles specifically — check whether it's actually `addresses` (LAZY) being accessed outside a transaction | Access lazy fields inside a `@Transactional` service method |

---

## 12. What's next

**Phase 3 is complete. Stopping here, as instructed.**

Phase 4 will build Store Management on top of this: `StoreController`
with create/update/view for `STORE_OWNER`s (using `@AuthenticationPrincipal`
exactly like `AuthController.me()` does), customer-facing store
search/filter, `ADMIN` management of all stores, and authorization
rules preventing one owner from editing another owner's store.

Say the word when you're ready to move to **Phase 4**.
