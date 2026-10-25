package com.nexora.phase11;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.idempotency.entity.IdempotencyKey;
import com.nexora.idempotency.entity.IdempotencyKeyStatus;
import com.nexora.idempotency.exception.IdempotencyConflictException;
import com.nexora.idempotency.repository.IdempotencyKeyRepository;
import com.nexora.idempotency.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService unit tests (Phase 11)")
class IdempotencyServiceTest {

    @Mock private IdempotencyKeyRepository repository;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(repository, new ObjectMapper());
    }

    @Test
    @DisplayName("A brand-new key runs the real action exactly once and caches the result")
    void newKeyRunsActionOnce() {
        when(repository.findByKeyValueAndScopeAndUserId("key-1", "TEST_SCOPE", 5L))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(IdempotencyKey.class)))
                .thenAnswer(inv -> {
                    IdempotencyKey k = inv.getArgument(0);
                    k.setId(100L);
                    return k;
                });
        when(repository.findById(100L)).thenReturn(Optional.of(
                IdempotencyKey.builder().id(100L).status(IdempotencyKeyStatus.IN_PROGRESS).build()));

        AtomicInteger callCount = new AtomicInteger(0);

        String result = idempotencyService.executeIdempotently(
                "key-1", "TEST_SCOPE", 5L, "fingerprint-A",
                new TypeReference<String>() {},
                () -> {
                    callCount.incrementAndGet();
                    return "the-real-result";
                });

        assertThat(result).isEqualTo("the-real-result");
        assertThat(callCount.get()).isEqualTo(1);
        verify(repository).save(argThat(k ->
                k.getStatus() == IdempotencyKeyStatus.COMPLETED
                        && k.getResponseBody() != null
                        && k.getResponseBody().contains("the-real-result")));
    }

    @Test
    @DisplayName("A COMPLETED key with a matching fingerprint replays the cached response WITHOUT running the action again")
    void completedKeyReplaysWithoutRerunning() {
        IdempotencyKey existingRecord = IdempotencyKey.builder()
                .id(200L).keyValue("key-2").scope("TEST_SCOPE").userId(5L)
                .requestFingerprint("fingerprint-A")
                .status(IdempotencyKeyStatus.COMPLETED)
                .responseBody("\"cached-result\"")
                .build();

        when(repository.findByKeyValueAndScopeAndUserId("key-2", "TEST_SCOPE", 5L))
                .thenReturn(Optional.of(existingRecord));

        AtomicInteger callCount = new AtomicInteger(0);

        String result = idempotencyService.executeIdempotently(
                "key-2", "TEST_SCOPE", 5L, "fingerprint-A",
                new TypeReference<String>() {},
                () -> {
                    callCount.incrementAndGet();
                    return "should-never-run";
                });

        assertThat(result).isEqualTo("cached-result");
        assertThat(callCount.get()).isZero(); // the real action was NEVER invoked
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("An IN_PROGRESS key rejects the duplicate with a conflict, without running the action")
    void inProgressKeyRejectsDuplicate() {
        IdempotencyKey inProgressRecord = IdempotencyKey.builder()
                .id(300L).keyValue("key-3").scope("TEST_SCOPE").userId(5L)
                .requestFingerprint("fingerprint-A")
                .status(IdempotencyKeyStatus.IN_PROGRESS)
                .build();

        when(repository.findByKeyValueAndScopeAndUserId("key-3", "TEST_SCOPE", 5L))
                .thenReturn(Optional.of(inProgressRecord));

        AtomicInteger callCount = new AtomicInteger(0);

        assertThatThrownBy(() -> idempotencyService.executeIdempotently(
                "key-3", "TEST_SCOPE", 5L, "fingerprint-A",
                new TypeReference<String>() {},
                () -> {
                    callCount.incrementAndGet();
                    return "should-never-run";
                }))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("already being processed");

        assertThat(callCount.get()).isZero();
    }

    @Test
    @DisplayName("The same key reused with a DIFFERENT fingerprint is rejected as a conflict")
    void differentFingerprintRejected() {
        IdempotencyKey existingRecord = IdempotencyKey.builder()
                .id(400L).keyValue("key-4").scope("TEST_SCOPE").userId(5L)
                .requestFingerprint("fingerprint-ORIGINAL")
                .status(IdempotencyKeyStatus.COMPLETED)
                .responseBody("\"original-result\"")
                .build();

        when(repository.findByKeyValueAndScopeAndUserId("key-4", "TEST_SCOPE", 5L))
                .thenReturn(Optional.of(existingRecord));

        assertThatThrownBy(() -> idempotencyService.executeIdempotently(
                "key-4", "TEST_SCOPE", 5L, "fingerprint-DIFFERENT",
                new TypeReference<String>() {},
                () -> "should-never-run"))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("different request");
    }

    @Test
    @DisplayName("If the real action throws, the IN_PROGRESS record is deleted so the key can be retried")
    void failedActionDeletesRecordForRetry() {
        when(repository.findByKeyValueAndScopeAndUserId("key-5", "TEST_SCOPE", 5L))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(IdempotencyKey.class)))
                .thenAnswer(inv -> {
                    IdempotencyKey k = inv.getArgument(0);
                    k.setId(500L);
                    return k;
                });

        assertThatThrownBy(() -> idempotencyService.executeIdempotently(
                "key-5", "TEST_SCOPE", 5L, "fingerprint-A",
                new TypeReference<String>() {},
                () -> { throw new RuntimeException("simulated failure"); }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("simulated failure");

        verify(repository).deleteById(500L);
        verify(repository, never()).save(any()); // never reached completeRecord
    }

    @Test
    @DisplayName("A concurrent duplicate insert (unique constraint violation) is treated as a conflict, not a crash")
    void concurrentInsertRaceHandledAsConflict() {
        when(repository.findByKeyValueAndScopeAndUserId("key-6", "TEST_SCOPE", 5L))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint violated"));

        assertThatThrownBy(() -> idempotencyService.executeIdempotently(
                "key-6", "TEST_SCOPE", 5L, "fingerprint-A",
                new TypeReference<String>() {},
                () -> "should-never-run"))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("already being processed");
    }
}
