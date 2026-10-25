package com.nexora.idempotency.repository;

import com.nexora.idempotency.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByKeyValueAndScopeAndUserId(String keyValue, String scope, Long userId);
}
