-- =============================================================================
-- NEXORA QUICK COMMERCE PLATFORM - V5 ADVANCED UPGRADE SCHEMA
-- Real-time Locations, Customer Feedback & Rider Ratings, Geofencing,
-- and Ledger-based Wallet System.
-- =============================================================================

-- 1. Real-time Delivery Locations & Telemetry Log
CREATE TABLE IF NOT EXISTS delivery_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_partner_id BIGINT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    speed_kmh DOUBLE DEFAULT 0.0,
    heading DOUBLE DEFAULT 0.0,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dl_partner (delivery_partner_id),
    INDEX idx_dl_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Customer Feedback & Rider Ratings
CREATE TABLE IF NOT EXISTS order_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    rider_id BIGINT,
    rating INT NOT NULL,
    review_tags VARCHAR(255),
    comment TEXT,
    tip_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    tip_status VARCHAR(50) NOT NULL DEFAULT 'NONE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_of_order (order_id),
    INDEX idx_of_customer (customer_id),
    INDEX idx_of_rider (rider_id),
    CONSTRAINT fk_of_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_of_customer FOREIGN KEY (customer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Customer Wallet Accounts
CREATE TABLE IF NOT EXISTS wallet_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_wallet_user (user_id),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Immutable Wallet Transactions Ledger
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    order_id BIGINT,
    transaction_uuid VARCHAR(64) NOT NULL UNIQUE,
    transaction_type VARCHAR(20) NOT NULL, -- CREDIT or DEBIT
    category VARCHAR(50) NOT NULL,         -- REFUND, CASHBACK, PAYMENT, PROMOTIONAL, TOPUP
    amount DECIMAL(10, 2) NOT NULL,
    balance_after DECIMAL(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED', -- COMPLETED, PENDING, FAILED
    idempotency_key VARCHAR(128) UNIQUE,
    reference_id VARCHAR(100),
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_wt_wallet (wallet_id),
    INDEX idx_wt_order (order_id),
    INDEX idx_wt_uuid (transaction_uuid),
    INDEX idx_wt_idempotency (idempotency_key),
    CONSTRAINT fk_wt_wallet FOREIGN KEY (wallet_id) REFERENCES wallet_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Dark Store Geofence & Location Reference Data (Idempotent seed/updates)
-- Ensure dark stores have exact Ahmedabad Coordinates and service radius
UPDATE stores SET description = CONCAT(COALESCE(description, ''), ' [GEO: 23.0373, 72.5118, 3.5km]') 
WHERE id = 4 AND (description IS NULL OR description NOT LIKE '%[GEO:%');
