package com.nexora.concurrency.model;

/**
 * Supported concurrency and locking strategies for inventory checkout and flash sales.
 */
public enum ConcurrencyStrategy {
    /**
     * Single atomic conditional SQL UPDATE (e.g. UPDATE inventory SET quantity = quantity - amount WHERE quantity >= amount).
     * High performance, database handles serialization, zero application-level lock overhead.
     */
    ATOMIC_UPDATE,

    /**
     * Database-level Pessimistic Write Lock (SELECT ... FOR UPDATE).
     * Locks the target row immediately; subsequent transactions queue up until commit/rollback.
     * Guarantees zero overselling; best when contention is very high (e.g. Flash sales with stock=1).
     */
    PESSIMISTIC_LOCK,

    /**
     * JPA @Version Optimistic Locking.
     * Reads row with version; detects collision on commit. First committer wins; subsequent transactions fail
     * with OptimisticLockException. Best when collision is infrequent (low to medium contention).
     */
    OPTIMISTIC_LOCK,

    /**
     * Redis Distributed Lock (SETNX / Redisson pattern).
     * Locks at the cache/coordination layer across multiple microservices or application nodes.
     * Prevents database connection pool exhaustion under massive flash sale traffic.
     */
    DISTRIBUTED_LOCK
}
