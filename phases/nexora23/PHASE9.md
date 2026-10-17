# NEXORA — Phase 9: Razorpay Test Payment

Continues directly from Phases 1-8. Two existing files touched, both
additively:

- `GlobalExceptionHandler` — two new `@ExceptionHandler` methods (same
  pattern every earlier phase used).
- `OrderService` (Phase 8) — two new methods, `markAwaitingPayment` and
  `getOrderEntityOrThrow` (see section 4).

Everything else is new: the full `payment` package, plus a test-only
stub (`StubPaymentGatewayClient`) and a Spring profile split explained
in section 2.

Still no Redis, Kafka, AI, microservices, or Docker. Signature
verification and webhooks are explicitly **Phase 10** — see section 3
for the exact boundary this phase stops at.

---

## 1. What was built

```
src/main/java/com/nexora/payment/
├── entity/
│   ├── Payment.java              <- one row per payment ATTEMPT (ManyToOne to Order)
│   ├── PaymentTransaction.java   <- append-only audit log per attempt
│   ├── PaymentStatus.java        <- CREATED, PENDING, SUCCESS, FAILED, REFUNDED
│   └── PaymentEventType.java     <- ORDER_CREATED, CLIENT_REPORTED_RESULT (Phase 9 only)
├── repository/
│   ├── PaymentRepository.java
│   └── PaymentTransactionRepository.java
├── gateway/
│   ├── PaymentGatewayClient.java       <- interface, see section 2
│   ├── RazorpayGatewayClientImpl.java  <- the ONLY class importing com.razorpay.*
│   ├── RazorpayOrderResult.java
│   └── PaymentGatewayException.java
├── dto/
│   ├── PaymentResponse.java
│   └── PaymentResultRequest.java
├── mapper/
│   └── PaymentMapper.java
├── exception/
│   └── InvalidPaymentStateException.java
├── service/
│   └── PaymentService.java
└── controller/
    └── PaymentController.java
```

New MySQL tables: `payments`, `payment_transactions` — matching names
already anticipated in the original database design (Phase 1).

---

## 2. Why a PaymentGatewayClient interface instead of calling the Razorpay SDK directly

Every earlier phase's tests run against real logic with zero external
dependencies — H2 instead of MySQL, no network calls at all. Payment
integration is the first phase where a real third-party service enters
the picture, and calling it directly from PaymentService would mean:

- Every `mvn test` run needs real Razorpay test credentials.
- Tests become slow and flaky (dependent on network/Razorpay uptime).
- CI pipelines need secrets just to run the test suite.

`PaymentGatewayClient` is a small interface
(`createOrder(amount, currency, receipt) -> RazorpayOrderResult`) that
PaymentService depends on instead. Two implementations exist:

| Implementation | Active when | Behavior |
|---|---|---|
| `RazorpayGatewayClientImpl` | `@Profile("!test")` — real app | Wraps the real `com.razorpay:razorpay-java` SDK |
| `StubPaymentGatewayClient` | `@Profile("test")` — test suite | Returns an instant fake result, no network at all |

`src/test/resources/application.properties` sets
`spring.profiles.active=test` for the whole suite, so every test class
(Phases 1-9, run together via `mvn test`) automatically gets the stub
— nothing about earlier phases' tests changed at all.

**A second safety net:** `RazorpayGatewayClientImpl` builds the real
`RazorpayClient` lazily, on first actual use, not in its constructor.
If it built eagerly, a missing/placeholder credential would fail the
entire application's startup — meaning nobody could even try Phases
1-8's endpoints without first setting up a Razorpay account. Built
lazily, the app always starts cleanly; only an actual
payment-initiation attempt fails (with a clean 502) until real
credentials are configured.

---

## 3. The exact scope boundary: Phase 9 vs. Phase 10

This is the most important thing to understand about this phase.

**Phase 9 (this phase) builds:**
- Creating a Razorpay order and storing a Payment row (PENDING).
- Moving the Nexora Order from CREATED to PAYMENT_PENDING.
- Recording whatever the frontend reports after Razorpay's Checkout
  widget closes (`POST /payments/{id}/result`) — purely as an
  unverified audit log entry.

**Phase 9 explicitly does NOT:**
- Verify Razorpay's cryptographic signature on anything.
- Ever set a Payment to SUCCESS.
- Ever move an Order to PAID or CONFIRMED.
- Handle Razorpay webhooks at all.

Why the line is drawn exactly there: Razorpay's Checkout widget runs
entirely in the customer's browser. By the time our backend hears
"payment_id: pay_XYZ, signature: abc123" from the frontend, that data
came from JavaScript running on a device we don't control. A
malicious or compromised client could call
`POST /payments/{id}/result` with a completely fabricated payment id
and claim success without ever paying anything — trusting it would let
anyone place free orders. This is exactly the "do not trust frontend
payment-success messages" principle the roadmap states explicitly for
Phase 10 — Phase 9 respects it by construction, simply by never wiring
the client-reported result to anything that matters.

Real confirmation requires verifying Razorpay's signature server-side,
which requires the shared secret and HMAC verification logic —
genuinely Phase 10's scope (Payment Verification & Webhooks). Until
then, in Nexora, a payment can only ever reach PENDING — getting to
SUCCESS requires Phase 10 to exist.

---

## 4. What changed in OrderService (Phase 8) — both additive

- **`markAwaitingPayment(Order order)`** — the system-driven
  CREATED -> PAYMENT_PENDING transition, called by PaymentService
  right after a Razorpay order is successfully created. Reuses the
  exact same state-machine validation (assertValidTransition) as
  updateStatus, just without the STORE_OWNER/ADMIN authorization gate
  — initiating payment is a customer/system action, not a
  store-management one. This is the same reasoning Phase 8 already
  applied to InventoryService.reduceStockForOrder/restoreStockForOrder.
- **`getOrderEntityOrThrow(Long orderId)`** — a public read-only
  wrapper around the existing private findOrderOrThrow, since
  PaymentService needs the real Order entity (to check ownership and
  current status), not an OrderResponse DTO.

Neither existing `updateStatus` nor `cancelOwnOrder` (Phase 8) changed
at all.

---

## 5. Endpoints

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/orders/{orderId}/payments` | order's owning customer, or ADMIN |
| POST | `/api/v1/payments/{paymentId}/result` | that payment's owning customer, or ADMIN |
| GET | `/api/v1/payments/{paymentId}` | owning customer, order's store owner, or ADMIN |
| GET | `/api/v1/orders/{orderId}/payments` | owning customer, order's store owner, or ADMIN |

### Sample: initiate payment

```
POST /api/v1/orders/42/payments
Authorization: Bearer <token>
```
```json
{
  "success": true,
  "message": "Payment initiated successfully",
  "data": {
    "id": 7,
    "orderId": 42,
    "razorpayOrderId": "order_JZ9r3vXvXvXvXv",
    "razorpayKeyId": "rzp_test_xxxxxxxxxx",
    "amount": 190.00,
    "currency": "INR",
    "status": "PENDING",
    "createdAt": "2026-08-23T10:00:00"
  }
}
```

`razorpayKeyId` is the PUBLIC key id — intentionally included so the
frontend can initialize Razorpay's Checkout.js widget with it. This is
exactly how Razorpay's own integration is meant to work; only
`razorpay.key-secret` is sensitive and never leaves the backend.

### Sample: record the client-reported result

```
POST /api/v1/payments/7/result
{ "razorpayPaymentId": "pay_ABC123", "razorpaySignature": "f4a2..." }
```
Returns 200 OK with the payment still PENDING — see section 3.

### Idempotency (a small preview of Phase 11)

Calling initiate-payment twice for the same order returns the SAME
Payment/Razorpay order instead of creating a duplicate — useful if a
customer refreshes the checkout page. Full idempotency-key handling
for retried/duplicate webhook deliveries is Phase 11's job; this is
just the simplest, most common case handled now.

---

## 6. Security — what's never stored, and what's public vs. secret

Per the roadmap's explicit requirement, this module never stores: a
card number, CVV, UPI PIN, or bank password. Structurally, it can't —
Payment and PaymentTransaction have no columns for any of those,
because Razorpay's hosted Checkout widget collects that information
directly on Razorpay's own page; it never reaches our frontend's
JavaScript or our backend at all.

| Value | Sensitivity | Where it lives |
|---|---|---|
| razorpay.key-id | Public | Backend config AND sent to frontend in PaymentResponse |
| razorpay.key-secret | Secret | Backend config only, environment variable, never in any response |
| Card/CVV/UPI/bank details | N/A | Never touches Nexora at all |

---

## 7. Run the Phase 9 tests

```bash
mvn test
```

Two new test classes:

1. **`PaymentServiceTest`** (Mockito unit tests) — successful
   initiation, rejecting an already-paid order, rejecting a
   non-CREATED order, the idempotent "return the existing pending
   payment" path, cross-customer authorization, ADMIN override, and —
   the most important test in this phase — directly proving that
   recording a client-reported result never changes the payment's
   status and never calls `paymentRepository.save(...)`.

2. **`PaymentIntegrationTest`** (full `@SpringBootTest` + `MockMvc`,
   real Spring Security chain, StubPaymentGatewayClient standing in
   for Razorpay) — the complete flow through real
   Cart -> Checkout -> Payment:
   - Initiating payment creates a real Payment, and the underlying
     Order really does transition to PAYMENT_PENDING (verified via a
     follow-up GET, not just the initiation response).
   - Calling initiate-payment twice returns the identical Razorpay
     order id, and only one Payment row exists.
   - Cross-customer rejection (403) and missing-token rejection (401).
   - Recording a client-reported result succeeds, but the order's
     status is confirmed — via a real database read — to still be
     PAYMENT_PENDING, not PAID.
   - Payment visibility: the order's store owner can see it, an
     unrelated customer cannot.

These run alongside every earlier phase's suite in the same `mvn test`
invocation.

---

## 8. Setting up real Razorpay test credentials (for running the app, not tests)

1. Create a free Razorpay account, switch to Test Mode (toggle in the
   dashboard).
2. Settings -> API Keys -> Generate Test Key. You'll get a key id
   (`rzp_test_...`) and a key secret — copy both immediately, the
   secret is only shown once.
3. Set them as environment variables before running the app:
   ```bash
   export RAZORPAY_KEY_ID=rzp_test_your_actual_key_id
   export RAZORPAY_KEY_SECRET=your_actual_key_secret
   mvn spring-boot:run
   ```
4. Razorpay's test mode provides dummy card numbers and UPI ids in
   their docs for simulating both successful and failed payments — no
   real money is ever involved.

---

## 9. Common errors & fixes (Phase 9 additions)

| Error | Cause | Fix |
|---|---|---|
| `502 Razorpay order creation failed: ...` | razorpay.key-id/key-secret still set to the placeholder defaults | Set real test-mode credentials (section 8) |
| `409 Cannot initiate payment for an order in status ...` | Order isn't CREATED (already paid, cancelled, etc.) | Check GET /api/v1/orders/{id} for its current status |
| `409 Order X has already been paid` | A SUCCESS payment already exists for this order | Nothing to do — it's already paid |
| Same razorpayOrderId returned on a second initiate-payment call | Expected — see the idempotency note in section 5 | Not an error |
| Order stuck at PAYMENT_PENDING, never becomes PAID | Correct and expected in Phase 9 — verification doesn't exist yet | Wait for Phase 10 |

---

## 10. What's next

**Phase 9 is complete. Stopping here, as instructed.**

Phase 10 (per the roadmap) builds real signature verification and
webhook handling: Razorpay -> Webhook -> Spring Boot -> Verify
Signature -> Update Payment -> Update Order, finally allowing a
Payment to reach SUCCESS and an Order to reach PAID/CONFIRMED —
through a cryptographically verified source, never a client claim. It
will also handle failed payments, invalid signatures, and duplicate
webhook deliveries.

Say the word when you're ready to move to **Phase 10**.
