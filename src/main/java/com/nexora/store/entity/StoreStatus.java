package com.nexora.store.entity;

/**
 * A store's operating status. Kept intentionally simple in Phase 2 —
 * store search/filtering logic that uses this arrives in Phase 4.
 */
public enum StoreStatus {
    OPEN,
    CLOSED,
    SUSPENDED
}
