# NEXORA — Phase 10: Payment Verification & Webhooks

Continues directly from Phases 1-9. This phase closes the loop Phase 9
deliberately left open — see PHASE9.md section 3, which explicitly
promised: "getting to SUCCESS requires Phase 10 to exist." It now does.

Files touched, all additively:
- `GlobalExceptionHandler` — one new `@ExceptionHandler`.
- `OrderService` (Phase 8) — one new method, `markPaid` (idempotent-safe).
- `SecurityConfig` (Phase 3) — one new path added to the existing
  `permitAll()` list (the webhook endpoint).
- `PaymentEventType` (Phase 9) — extended with 6 new values for the
  events this phase now actually handles.
- `PaymentService` (Phase 9) — `recordClientReportedResult` now
  actually verifies a signature instead of only logging; a new
  `handleWebhookEvent` method added.
- One Phase 9 test method updated to match the new, correct,
  intentionally-different behavior (see section 7).

New: `RazorpaySignatureVerifier`, `PaymentWebhookController`, and the
`payment.security` package.

Still no Redis, Kafka, AI, microservices, or Docker.

---

## 1. The two signatures, and why they're different

Razorpay offers two independent cryptographic confirmations, for two
different transport paths:

| | Client-callback signature | Webhook signature |
|---|---|---|
| Who sends it | Razorpay's Checkout widget, to the browser | Razorpay's servers, directly to our backend |
| Depends on | The customer's browser successfully calling back | Nothing client-side at all |
| Signed data | "{razorpay_order_id}\|{razorpay_payment_id}" | The exact raw webhook request body |
| Secret used | razorpay.key-secret (same one used to create the order) | razorpay.webhook-secret (a SEPARATE secret, configured in the Razorpay dashboard specifically for webhooks) |
| Verified in | PaymentService.recordClientReportedResult | PaymentService.handleWebhookEvent |

Both are genuine, independent proof that data came from Razorpay —
neither is "trusting the frontend." The webhook path is the more
resilient one (a production system should treat it as authoritative
even if the client-callback path also fires), which is exactly why the
roadmap's flow diagram centers on it: Razorpay -> Webhook -> Spring
Boot -> Verify Signature -> Update Payment -> Update Order.

`RazorpaySignatureVerifier` implements both, using HMAC-SHA256 and a
constant-time comparison (`MessageDigest.isEqual`, not `.equals()`) —
see its javadoc for why timing-safe comparison matters for signature
verification specifically.

---

## 2. What changed in PaymentService.recordClientReportedResult

Phase 9: logged whatever the client sent, always returned 200, never
changed anything.

Phase 10: verifies the signature first.
- Valid -> calls the shared `applySuccessfulPayment` — payment becomes
  SUCCESS, order becomes PAID (via OrderService.markPaid).
- Invalid -> logs a CLIENT_SIGNATURE_INVALID transaction and **throws**
  InvalidPaymentSignatureException (400). This is a behavior change
  from Phase 9's "silently ignore and stay PENDING" — an invalid
  signature is now treated as the security-relevant event it is, not
  a routine no-op.

---

## 3. The webhook endpoint

`POST /api/v1/payments/webhook` — public, no JWT (added to
SecurityConfig's existing permitAll() list). Razorpay's servers call
this directly; they have no Nexora user account and never will. The
endpoint's entire security model is the signature check, not Spring
Security's usual authentication — this is the standard, correct way
webhooks are secured industry-wide.

```
Headers:  X-Razorpay-Signature: <hex HMAC-SHA256 of the raw body>
Body:     { "event": "payment.captured", "payload": { "payment": { "entity": {
              "id": "pay_XXXX", "order_id": "order_XXXX", "status": "captured"
          }}}}
```

Why the controller binds `@RequestBody String`, not a DTO: the
signature is computed over the exact raw bytes Razorpay sent.
Deserializing into a typed object first and re-serializing to check
the signature could silently reorder keys or change whitespace,
breaking verification for reasons that have nothing to do with whether
the webhook is genuine. Binding a plain String guarantees we verify
against precisely what arrived.

### handleWebhookEvent step by step

1. Verify the signature over the raw body. Invalid -> throw
   immediately, before parsing or touching any payload data at all.
   An unverified body could claim to be about any order; acting on it
   before verification is exactly what "do not trust... payment
   messages" forbids.
2. Parse event, order_id, payment_id from the (now-verified) body.
3. Look up the Payment by razorpayOrderId. Not found -> 404.
4. Duplicate check: has a PaymentTransaction with this exact event
   type + razorpayPaymentId already been recorded for this payment? If
   so, log WEBHOOK_DUPLICATE_IGNORED and return — this handles
   Razorpay's at-least-once delivery guarantee (the same event can
   arrive more than once).
5. payment.captured -> applySuccessfulPayment (payment SUCCESS, order
   PAID). payment.failed -> payment FAILED, order status LEFT
   UNTOUCHED (still PAYMENT_PENDING — a declined card is routine and
   retryable; auto-cancelling on the first failure would be
   unnecessarily punishing). Any other event type -> acknowledged with
   no action (signature was valid, we simply have nothing to do with
   it yet — refund events are reserved for Phase 11).

---

## 4. The one shared success path: applySuccessfulPayment

Both verification routes — client-callback and webhook — funnel
through the exact same private method once a signature checks out:

```java
private void applySuccessfulPayment(Payment payment, String razorpayPaymentId,
                                     PaymentEventType eventType, String note) {
    payment.setStatus(PaymentStatus.SUCCESS);
    paymentRepository.save(payment);
    paymentTransactionRepository.save(...);
    orderService.markPaid(payment.getOrder());   // idempotent-safe
}
```

This is the ONLY place in the entire application that sets a Payment
to SUCCESS. Everything upstream of it (client signature check, webhook
signature check, the duplicate-event check) exists purely to decide
whether this method should run — once it runs, both paths behave
identically.

`OrderService.markPaid` is deliberately idempotent: if the order is
already PAID, it silently does nothing instead of throwing. This is a
second layer of duplicate-safety, in case a redundant call somehow
reached it despite the transaction-log duplicate check above —
calling it twice must never corrupt state or throw a 500.

---

## 5. Endpoints

| Method | Path | Access |
|---|---|---|
| POST | /api/v1/payments/webhook | public — secured by signature, not JWT |
| POST | /api/v1/payments/{paymentId}/result | owning customer or ADMIN (unchanged path from Phase 9; behavior changed, see section 2) |

All other Phase 9 payment endpoints are unchanged.

---

## 6. Run the Phase 10 tests

```bash
mvn test
```

Two new test classes:

1. **RazorpaySignatureVerifierTest** (pure unit tests, no Spring
   context) — a correctly-computed signature is accepted; a signature
   computed with the wrong secret is rejected; a signature for
   different order/payment ids is rejected; garbage/null/blank input
   is rejected without throwing; a webhook signature is rejected if
   even one character of the body changes after signing; and —
   directly proving the two-secret design — a signature valid under
   key-secret does NOT validate as a webhook signature over the same
   content.

2. **PaymentVerificationIntegrationTest** (full @SpringBootTest +
   MockMvc, real Spring Security chain, REAL HMAC-SHA256 computation —
   no mocking of the verifier here) — the complete flow through real
   Cart -> Checkout -> Payment -> Verification:
   - A valid client-callback signature -> payment SUCCESS, order PAID
     (verified via a follow-up GET, not just the response body).
   - Calling the result endpoint twice with the identical valid
     signature doesn't error (idempotency).
   - A valid webhook payment.captured -> SUCCESS/PAID.
   - A valid webhook payment.failed -> payment FAILED, order confirmed
     still PAYMENT_PENDING (not auto-cancelled).
   - An invalid webhook signature -> 400, order confirmed untouched.
   - A body tampered with after signing (a genuinely valid signature
     for the ORIGINAL body, sent alongside a MODIFIED body) -> 400 —
     this is the core webhook-tampering scenario.
   - A duplicate webhook delivery of the identical event -> second
     call still returns 200 (not an error), and the order is confirmed
     still correctly PAID, not corrupted by reprocessing.
   - A validly-signed webhook for an order id that doesn't exist -> 404.
   - The webhook endpoint succeeds with NO Authorization header at
     all, confirming it's genuinely public and secured by signature
     alone.

Plus one Phase 9 test updated (see section 7) — the full suite across
all 10 phases runs together in one `mvn test` invocation.

---

## 7. One Phase 9 test intentionally updated — and why that's correct

`PaymentIntegrationTest.clientReportedResultDoesNotAdvanceOrder`
(Phase 9) sent a fake signature and asserted 200 OK with the payment
staying PENDING. Phase 9's own documentation explicitly flagged this
as temporary: "Phase 10 will verify this signature." Now that
verification is real, a fake signature is correctly rejected (400),
not silently accepted.

Rather than leave a test asserting behavior that's now wrong, the one
affected method was updated (renamed to
clientReportedResultWithInvalidSignatureIsRejected, comment explains
why) to assert the new, correct outcome. Every other test in that
file — initiation, idempotency, authorization, visibility — is
completely unaffected and still passes unchanged. This is a deliberate
behavior evolution across phases, the same kind of change Phase 8 made
to Phase 4's deleteProduct (adding a guard) — not an unrelated
regression.

`PaymentServiceTest` (Phase 9, unit tests) also needed a mechanical
update: PaymentService's constructor gained two new dependencies
(RazorpaySignatureVerifier, ObjectMapper), so the test's manual
`new PaymentService(...)` call needed the two new (mocked) arguments
to keep compiling. Its one behavioral test
(clientReportedResultWithInvalidSignatureIsRejected) now stubs
`signatureVerifier.verifyPaymentSignature(...)` to return false and
asserts the throw — mirroring the same real behavior the integration
test proves end-to-end with a genuine HMAC computation.

---

## 8. Common errors & fixes (Phase 10 additions)

| Error | Cause | Fix |
|---|---|---|
| 400 on a client-result call you expected to succeed | The signature doesn't match — likely wrong razorpayPaymentId/razorpaySignature pairing, or razorpay.key-secret misconfigured | Confirm you're passing exactly what Razorpay's Checkout callback gave you |
| 400 on a webhook you configured in the Razorpay dashboard | razorpay.webhook-secret doesn't match what's configured for that webhook in the dashboard | Regenerate/copy the webhook secret from Razorpay Settings -> Webhooks and update the environment variable |
| 404 on a webhook call | No Payment exists with that razorpay_order_id — often means testing with a stale/unrelated order id | Confirm the webhook is for an order that actually went through POST /orders/{id}/payments first |
| Webhook signature always fails locally when testing manually (e.g. via curl/Postman) | Any reformatting of the JSON body (pretty-printing, re-ordering) changes the bytes being signed | Send the exact raw body you computed the signature over — don't let your HTTP client "beautify" it |
| Order stuck at PAYMENT_PENDING even after a successful test payment | Confirmation requires either a valid client-callback call OR a valid webhook delivery — if testing manually without a real Razorpay account, you need to simulate one of those two calls yourself with a correctly computed signature | See PaymentVerificationIntegrationTest for exactly how a valid signature is computed |

---

## 9. What's next

**Phase 10 is complete. Stopping here, as instructed.**

Phase 11 (per the roadmap) builds Payment Reliability on top of this:
proper refund flow (using the REFUNDED PaymentStatus value that's
existed since Phase 9 but has no code path to it yet), more general
idempotency-key handling for retried requests (this phase only handled
the two most common cases — duplicate webhooks and repeated
client-result calls — by content-matching, not a dedicated
idempotency-key table), and explicit failure-scenario testing for
network partitions between payment success and order confirmation.

Say the word when you're ready to move to **Phase 11**.
