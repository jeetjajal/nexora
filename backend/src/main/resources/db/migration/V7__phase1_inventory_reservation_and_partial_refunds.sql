-- =============================================================================
-- NEXORA QUICK COMMERCE PLATFORM - V7 PHASE 1 CORE TRANSACTION RELIABILITY
-- Inventory Reservation Engine, Dark Store Geocoordinates, and Partial Refunds.
-- =============================================================================

-- 1. Inventory Reservations Table (10-minute TTL Two-Phase Commit)
CREATE TABLE IF NOT EXISTS inventory_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    batch_id BIGINT NULL,
    store_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RESERVED', -- RESERVED, COMMITTED, RELEASED, EXPIRED
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ir_order (order_id),
    INDEX idx_ir_product (product_id),
    INDEX idx_ir_batch (batch_id),
    INDEX idx_ir_status_expires (status, expires_at),
    CONSTRAINT fk_ir_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_ir_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Add Geographic Coordinates to Stores Table
ALTER TABLE stores ADD COLUMN IF NOT EXISTS latitude DOUBLE NULL;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS longitude DOUBLE NULL;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS service_radius_km DOUBLE DEFAULT 4.0;

-- 3. Seed Authoritative Ahmedabad Dark Store Coordinates
UPDATE stores SET latitude = 23.0373, longitude = 72.5118, service_radius_km = 4.0 WHERE id = 4;
UPDATE stores SET latitude = 23.0350, longitude = 72.5293, service_radius_km = 3.5 WHERE id = 2;
UPDATE stores SET latitude = 23.0125, longitude = 72.5075, service_radius_km = 3.5 WHERE id = 1;
UPDATE stores SET latitude = 23.0340, longitude = 72.4630, service_radius_km = 4.5 WHERE id = 3;

-- 4. Add Refund Tracking Column to Payments Table
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refunded_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00;
