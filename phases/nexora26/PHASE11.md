# NEXORA — Phase 11: Payment Reliability

Continues directly from Phases 1-10. Files touched, all additively:

- GlobalExceptionHandler - one new @ExceptionHandler.
- OrderService (Phase 8) - one new field (IdempotencyService), one
  new checkout overload, one new method (markCancelledDueToRefund).
- PaymentService (Phase 9/10) - a real bug fix to retry eligibility,
  one new field, one new initiatePayment overload, one new method
  (refundPayment).
- PaymentController/OrderController - one new optional header, one
  new endpoint (/payments/{id}/refund).
- PaymentGatewayClient/RazorpayGatewayClientImpl/StubPaymentGatewayClient
  (Phase 9) - one new method (refundPayment).
- Payment entity (Phase 9) - one new column (razorpayPaymentId).
- PaymentEventType (Phase 9/10) - one new value (REFUND_COMPLETED).
- Two existing tests (OrderStatusTransitionTest, PaymentServiceTest)
  needed a mechanical update: both constructors they manually invoke
  gained a new dependency.

New: the idempotency package.

Still no Redis, Kafka, AI, microservices, or Docker.

---

## 1. Three real gaps, closed

This phase isn't just new scaffolding bolted on - it closes three
genuine problems that existed in the Phase 9/10 code:

1. **A real bug: payment retry didn't work.** Phase 10 deliberately
   left a failed order at PAYMENT_PENDING rather than auto-cancelling
   it (a declined card should be retryable). But initiatePayment
   only allowed a NEW attempt from CREATED - so a customer whose
   card was declined had NO WAY to try again. Fixed: eligibility
   now also accepts PAYMENT_PENDING (see section 3).
2. **No formal duplicate-request protection**, despite the roadmap's
   exact scenario ("customer pays, network fails, customer retries").
   Phase 9's "return the existing PENDING payment" check only covers
   ONE narrow case (retrying payment initiation) and only after a
   Payment row already exists - it does nothing for checkout itself.
   Fixed: a general-purpose Idempotency-Key mechanism (section 2).
3. **REFUNDED was a PaymentStatus value with no code path to it**
   since Phase 9. Fixed: a real refund flow (section 4).

---

## 2. Idempotency-Key - the general mechanism

**The scenario, exactly as the roadmap states it:** a customer submits
checkout (or payment initiation), the network fails before their
browser sees the response, and they retry. Did the first request
actually succeed server-side? The client can't know - and blindly
retrying would create a second order or a second Razorpay order.

**The fix:** the client sends a self-generated Idempotency-Key header
(any unique string - a UUID is the usual choice). The first time a key
is seen, the real operation runs and its result is cached. Every
subsequent request with the SAME key gets the cached result
replayed - the operation is NEVER run a second time, not because
we cleverly detected a duplicate order afterward, but because the
retry never reaches the business logic again at all.

```
POST /api/v1/orders/checkout
Idempotency-Key: 3f29a1e0-...

{ "deliveryAddressId": 12 }
```

**Why a unique database constraint, not just an in-memory or
application-level check:** picture the network failure wasn't total -
the first request is still genuinely in flight (e.g. a slow DB round
trip) when the retry arrives. An application-level "does a record
exist?" check has a race: both requests could check at nearly the same
instant, both see "not yet," and both proceed. The UNIQUE constraint
on (key_value, scope, user_id) closes that race at the database
level - only one of two simultaneous INSERT attempts for the same
key can ever succeed, regardless of application-level timing. This is
the same principle Phase 6 relied on for atomic inventory updates.

**Three outcomes** for a given key:

| State found | Fingerprint | Result |
|---|---|---|
| None | - | Run the real operation; cache the result |
| COMPLETED | Matches | Replay the cached response - operation never re-runs |
| IN_PROGRESS | - | 409 - a duplicate arrived while the original is still executing |
| Any | Doesn't match | 409 - same key reused for a materially different request (a client bug) |

**"Fingerprint"?** A short summary of what the request is actually
about - for checkout, the deliveryAddressId; for payment
initiation, the orderId. If the same key arrives with a DIFFERENT
fingerprint, that's not a legitimate retry - it's a client bug (key
reuse across unrelated requests) - and it's rejected rather than
silently replaying the wrong cached response.

**The header is optional.** Omitting it behaves exactly as it did
before this phase - no tracking, no behavior change at all. Wired into
POST /orders/checkout and POST /orders/{id}/payments.

**On failure, the key is released.** If the real operation throws, the
IN_PROGRESS record is deleted, not kept around - a failed attempt
must never permanently "burn" a key and block all future retries with
it.

---

## 3. Payment retry - the fix

PaymentService.initiatePayment's eligibility check now accepts
CREATED OR PAYMENT_PENDING (previously only CREATED), as
long as no payment is currently SUCCESS or actively
PENDING/CREATED. This is exactly what makes a retry-after-failure
possible: the order sits at PAYMENT_PENDING after a declined card
(Phase 10's deliberate design), and now a fresh POST
/orders/{id}/payments call is allowed to create a genuinely new
Payment attempt (a different razorpayOrderId) instead of being
rejected with "expected CREATED."

GET /orders/{id}/payments shows the full history - every attempt,
including the failed one(s), stays visible for audit.

---

## 4. Refund flow

POST /api/v1/payments/{paymentId}/refund - full refund of a
SUCCESS payment via the gateway's refund API (mirroring createOrder's
pattern: PaymentGatewayClient.refundPayment, with
StubPaymentGatewayClient returning an instant fake result in tests,
same as Phase 9's createOrder stub).

**Authorization: the order's STORE_OWNER, or ADMIN - never the
customer.** This follows the same "backend stays financially
authoritative" philosophy that's run through the whole project (never
trusting a client-supplied price, never trusting a client-reported
payment success). A customer who wants a refund goes through their
store/support, same as most real platforms - there's no self-service
refund endpoint.

**On success:**
1. Payment.status -> REFUNDED.
2. A REFUND_COMPLETED PaymentTransaction is logged.
3. OrderService.markCancelledDueToRefund - idempotent and
   forgiving: transitions the order to CANCELLED if reachable from
   its current status, but silently does nothing otherwise (e.g. an
   already-terminal order). By the time this runs, real money has
   already moved back to the customer via the gateway - that fact is
   authoritative; failing the whole refund over an order-bookkeeping
   technicality would be worse than leaving the order's status as-is
   in a rare edge case.
4. **Stock is restored** - consistent with every other path that
   reaches CANCELLED in this system (Phase 8's updateStatus and
   cancelOwnOrder both restore stock; this needed the same fix,
   caught and applied before writing any tests against it).

**Eligibility guards:** only a SUCCESS payment can be refunded
(409 otherwise); a payment can't be refunded twice (409 on the
second attempt, since it's no longer SUCCESS).

**Scope boundary:** full refunds only (no partial refunds), and the
outcome is trusted directly from the gateway API's synchronous
response rather than confirmed via a refund.processed webhook. A
production system would likely also listen for that webhook as the
authoritative confirmation (the same reasoning Phase 10 applied to
payments) - a natural next increment, not implemented here to keep
this phase focused.

---

## 5. Endpoints

| Method | Path | Access | New in Phase 11 |
|---|---|---|---|
| POST | /api/v1/orders/checkout | any authenticated | optional Idempotency-Key header |
| POST | /api/v1/orders/{orderId}/payments | order's owning customer, or ADMIN | optional Idempotency-Key header; retry now genuinely possible |
| POST | /api/v1/payments/{paymentId}/refund | order's STORE_OWNER, or ADMIN | new endpoint |

---

## 6. Run the Phase 11 tests

```bash
mvn test
```

Three new test classes:

1. **IdempotencyServiceTest** (Mockito unit tests) - a brand-new key
   runs the action exactly once; a COMPLETED key with a matching
   fingerprint replays the cached response WITHOUT re-running the
   action; an IN_PROGRESS key is rejected; a mismatched fingerprint
   is rejected; a failed action deletes its record (so the same key
   can be retried); a DataIntegrityViolationException (the
   real-world signal of a concurrent race) is translated into a clean
   conflict, not a crash.

2. **PaymentReliabilityIntegrationTest** (full @SpringBootTest +
   MockMvc, real Spring Security, real HMAC computation) - the
   complete flow:
   - Retry after failure: initiate -> webhook payment.failed ->
     order confirmed still PAYMENT_PENDING -> retry succeeds with a
     genuinely new razorpayOrderId -> webhook payment.captured ->
     order reaches PAID -> both attempts visible in payment history.
   - Idempotent checkout: same key replayed -> identical order
     returned, stock reduced only once, only one order exists.
   - Checkout without a key -> unchanged Phase 8 behavior (regression check).
   - Same key, different address -> 409 (fingerprint mismatch).
   - Idempotent payment initiation -> same key replayed -> identical
     payment, only one attempt ever created.
   - Refund: store owner refunds a SUCCESS payment -> REFUNDED,
     order CANCELLED, stock restored (verified via a real database
     read, before and after).
   - Refund authorization: customer -> 403; a DIFFERENT store
     owner -> 403; ADMIN -> succeeds.
   - Refund eligibility: a non-SUCCESS payment -> 409; a second
     refund on the same payment -> 409.

3. **IdempotencyConcurrencyIntegrationTest** (real multi-threading,
   mirroring Phase 6's InventoryConcurrencyIntegrationTest exactly)
   - this is the test that actually proves the guarantee under real
   concurrency, not just sequential retries: 10 genuine threads fire
   checkout with the identical Idempotency-Key at the same instant,
   through the real Spring-managed OrderService bean and real
   database transactions. Regardless of how the 10 threads split
   between "succeeded" and "409 conflict" (non-deterministic, and the
   test doesn't require a specific split), the core invariant is
   verified directly against the database: exactly one order was
   created, and stock was reduced exactly once - never more.

These run alongside every earlier phase's suite in the same mvn test
invocation.

---

## 7. Common errors & fixes (Phase 11 additions)

| Error | Cause | Fix |
|---|---|---|
| 409 on a checkout retry you expected to just work | Same Idempotency-Key sent with a DIFFERENT deliveryAddressId than the original attempt | Reuse a key only for byte-for-byte the same logical request; generate a new key for a genuinely different one |
| 409 "already being processed" on a request you only sent once | A previous request with that exact key is still IN_PROGRESS (or crashed mid-flight without cleanup - rare) | Wait briefly and retry with a NEW key if this persists |
| 409 initiating payment even after a failure | Another payment for this order is already SUCCESS or actively PENDING | Check GET /orders/{id}/payments for the order's current payment history |
| 409 on a refund | Either the payment isn't SUCCESS yet, or it's already REFUNDED | Check GET /payments/{id} for its current status |
| 403 on a refund attempt | Refunds are STORE_OWNER (of that order's store) or ADMIN only - never the customer | Have the store or an admin issue the refund instead |

---

## 8. What's next

**Phase 11 is complete. Stopping here, as instructed.**

Phase 12 (per the roadmap) builds the Delivery System: delivery
partner profiles, online/offline/available/busy status, and - once an
order reaches READY - nearest-available-rider assignment.

Say the word when you're ready to move to **Phase 12**.
