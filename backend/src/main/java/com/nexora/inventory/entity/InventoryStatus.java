package com.nexora.inventory.entity;

/**
 * NOT a persisted column — this is a DERIVED value, computed from
 * quantity vs. lowStockThreshold (see InventoryMapper). We don't store
 * "status" in the database because it would just be redundant data
 * that could drift out of sync with the real quantity (e.g. if
 * something updated quantity directly without remembering to also
 * update a stored status column). Deriving it fresh every time a
 * response is built guarantees it's always consistent with the actual
 * quantity.
 */
public enum InventoryStatus {
    OUT_OF_STOCK,
    LOW_STOCK,
    IN_STOCK
}
