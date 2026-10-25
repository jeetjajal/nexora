package com.nexora.idempotency.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.idempotency.entity.IdempotencyKey;
import com.nexora.idempotency.entity.IdempotencyKeyStatus;
import com.nexora.idempotency.exception.IdempotencyConflictException;
import com.nexora.idempotency.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * SEE IdempotencyKey's javadoc FIRST for the full "why" — this class
 * is the mechanical "how."
 *
 * USAGE (from OrderService.checkout / PaymentService.initiatePayment):
 *   idempotencyService.executeIdempotently(
 *       key, "CHECKOUT", userId, fingerprint,
 *       new TypeReference&lt;List&lt;OrderResponse&gt;&gt;() {},
 *       () -&gt; theRealOperation());
 *
 * THREE OUTCOMES for a given (key, scope, userId):
 *   1. NEVER SEEN BEFORE      -&gt; insert an IN_PROGRESS record, run the
 *      real action. On success, cache the result and mark COMPLETED.
 *      On failure, DELETE the record (so the same key can be retried
 *      after a genuine failure — a failed attempt should never
 *      permanently "burn" a key) and let the original exception propagate.
 *   2. SEEN BEFORE, COMPLETED, SAME fingerprint -&gt; this is a genuine
 *      retry of the same logical request; the real action is NEVER
 *      run again — the cached response is deserialized and returned directly.
 *   3. SEEN BEFORE, IN_PROGRESS, or SAME key with a DIFFERENT
 *      fingerprint -&gt; IdempotencyConflictException (409); see that
 *      exception's javadoc for the two distinct cases this covers.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public <T> T executeIdempotently(
            String idempotencyKey,
            String scope,
            Long userId,
            String requestFingerprint,
            TypeReference<T> responseType,
            Supplier<T> action) {

        Optional<IdempotencyKey> existing =
                repository.findByKeyValueAndScopeAndUserId(idempotencyKey, scope, userId);

        if (existing.isPresent()) {
            return handleExistingRecord(existing.get(), requestFingerprint, responseType);
        }

        IdempotencyKey record = beginNewRecord(idempotencyKey, scope, userId, requestFingerprint);

        T result;
        try {
            result = action.get();
        } catch (RuntimeException ex) {
            // The real operation failed — remove the IN_PROGRESS record
            // so the SAME key can be used again for a fresh attempt.
            // A failed attempt must never permanently lock out retries.
            repository.deleteById(record.getId());
            throw ex;
        }

        completeRecord(record, result);
        return result;
    }

    private <T> T handleExistingRecord(IdempotencyKey record, String requestFingerprint, TypeReference<T> responseType) {
        if (!record.getRequestFingerprint().equals(requestFingerprint)) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key '" + record.getKeyValue() + "' was already used for a different request");
        }

        if (record.getStatus() == IdempotencyKeyStatus.IN_PROGRESS) {
            throw new IdempotencyConflictException(
                    "A request with Idempotency-Key '" + record.getKeyValue() + "' is already being processed");
        }

        // COMPLETED with a matching fingerprint — this is a genuine
        // retry. Replay the cached response WITHOUT running the
        // operation again.
        try {
            return objectMapper.readValue(record.getResponseBody(), responseType);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to replay cached idempotent response", ex);
        }
    }

    /**
     * IMPORTANT IMPLEMENTATION NOTE: this method deliberately does NOT
     * carry its own @Transactional annotation. Spring's @Transactional
     * only takes effect when a method is called THROUGH the bean's
     * Spring-managed proxy — calling it here, from another method in
     * the SAME class (self-invocation), bypasses that proxy entirely,
     * so an annotation here would be silently ignored anyway (a classic
     * Spring AOP pitfall). We don't need it: Spring Data JPA's
     * repository methods (like saveAndFlush below) are ALREADY
     * individually transactional by default — each call commits (or
     * fails) as its own unit. That's exactly the "commit immediately,
     * don't wait for the outer business action to finish" behavior
     * IdempotencyKey's javadoc describes: the INSERT's unique-constraint
     * check is enforced and visible to other transactions the moment
     * this call returns, closing the race window described there.
     */
    private IdempotencyKey beginNewRecord(String idempotencyKey, String scope, Long userId, String requestFingerprint) {
        IdempotencyKey record = IdempotencyKey.builder()
                .keyValue(idempotencyKey)
                .scope(scope)
                .userId(userId)
                .requestFingerprint(requestFingerprint)
                .status(IdempotencyKeyStatus.IN_PROGRESS)
                .build();

        try {
            return repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            // Someone else's concurrent request won the race to insert
            // this exact (key, scope, userId) combination first.
            throw new IdempotencyConflictException(
                    "A request with Idempotency-Key '" + idempotencyKey + "' is already being processed");
        }
    }

    /** See beginNewRecord's note on self-invocation — same reasoning applies here. */
    private <T> void completeRecord(IdempotencyKey record, T result) {
        try {
            String serialized = objectMapper.writeValueAsString(result);
            IdempotencyKey managed = repository.findById(record.getId()).orElseThrow();
            managed.setResponseBody(serialized);
            managed.setStatus(IdempotencyKeyStatus.COMPLETED);
            repository.save(managed);
        } catch (Exception ex) {
            // The real operation already succeeded (e.g. the order was
            // genuinely created) — a failure to CACHE that result
            // shouldn't be reported to the client as if their request
            // failed. Worst case, a retried request with this key
            // re-runs the action once more rather than replaying a
            // cached response; that's a lesser problem than telling a
            // customer their successful order failed.
        }
    }
}
