-- =============================================================================
-- NEXORA QUICK COMMERCE PLATFORM - V6 ENTERPRISE UPGRADE SCHEMA
-- Multi-Order Delivery Batching, WhatsApp Conversational Commerce,
-- Cold Chain & FEFO Batch Inventory, Rider Shift KYC, and Mobile Telemetry.
-- =============================================================================

-- 1. Multi-Order Delivery Batches
CREATE TABLE IF NOT EXISTS delivery_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_code VARCHAR(64) NOT NULL UNIQUE,
    store_id BIGINT NOT NULL,
    delivery_partner_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED', -- CREATED, ASSIGNED, IN_TRANSIT, COMPLETED, CANCELLED
    order_count INT NOT NULL DEFAULT 0,
    total_distance_km DECIMAL(6, 2) NOT NULL DEFAULT 0.00,
    estimated_duration_mins INT NOT NULL DEFAULT 0,
    dispatched_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_db_store (store_id),
    INDEX idx_db_partner (delivery_partner_id),
    INDEX idx_db_status (status),
    CONSTRAINT fk_db_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_db_partner FOREIGN KEY (delivery_partner_id) REFERENCES delivery_partners (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Delivery Batch Order Stop Mapping
CREATE TABLE IF NOT EXISTS delivery_batch_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL UNIQUE,
    sequence_index INT NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_TRANSIT, DELIVERED, FAILED
    estimated_delivery_time TIMESTAMP NULL,
    actual_delivery_time TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dbo_batch (batch_id),
    INDEX idx_dbo_order (order_id),
    CONSTRAINT fk_dbo_batch FOREIGN KEY (batch_id) REFERENCES delivery_batches (id) ON DELETE CASCADE,
    CONSTRAINT fk_dbo_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. WhatsApp Conversational Commerce Sessions
CREATE TABLE IF NOT EXISTS whatsapp_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wa_phone_number VARCHAR(30) NOT NULL UNIQUE,
    user_id BIGINT,
    customer_name VARCHAR(120),
    current_state VARCHAR(40) NOT NULL DEFAULT 'IDLE', -- IDLE, PARSING_ITEMS, AWAITING_ADDRESS, AWAITING_CONFIRMATION, CHECKOUT_PENDING, ORDER_PLACED
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'en', -- en, gu, hi
    session_context TEXT, -- JSON holding active drafted cart, address, resolution items
    last_active_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_wac_phone (wa_phone_number),
    INDEX idx_wac_user (user_id),
    CONSTRAINT fk_wac_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. WhatsApp Message Audit Ledger
CREATE TABLE IF NOT EXISTS whatsapp_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    direction VARCHAR(20) NOT NULL, -- INBOUND, OUTBOUND
    message_id VARCHAR(100),
    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT', -- TEXT, INTERACTIVE, TEMPLATE, ORDER_LINK
    message_body TEXT,
    raw_payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_wam_conv (conversation_id),
    CONSTRAINT fk_wam_conv FOREIGN KEY (conversation_id) REFERENCES whatsapp_conversations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. FEFO Batch Inventory Ledger
CREATE TABLE IF NOT EXISTS inventory_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    batch_number VARCHAR(60) NOT NULL,
    mfg_date DATE NULL,
    expiry_date DATE NOT NULL,
    initial_quantity INT NOT NULL DEFAULT 0,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    unit_cost DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    temperature_zone VARCHAR(30) NOT NULL DEFAULT 'AMBIENT', -- AMBIENT, CHILLED_2_TO_8, FROZEN_MINUS_18
    is_quarantined BOOLEAN NOT NULL DEFAULT FALSE,
    discount_applied_percent INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ib_product (product_id),
    INDEX idx_ib_store (store_id),
    INDEX idx_ib_expiry (expiry_date),
    INDEX idx_ib_zone (temperature_zone),
    CONSTRAINT fk_ib_prod FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_ib_store FOREIGN KEY (store_id) REFERENCES stores (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Cold Chain Temperature Telemetry Logs
CREATE TABLE IF NOT EXISTS temperature_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    storage_unit VARCHAR(60) NOT NULL, -- CHILLER_01, FREEZER_01, TRANSIT_COOLBOX_12
    temperature_celsius DOUBLE NOT NULL,
    humidity_percentage DOUBLE,
    min_safe_temp DOUBLE NOT NULL DEFAULT 2.0,
    max_safe_temp DOUBLE NOT NULL DEFAULT 8.0,
    breach_detected BOOLEAN NOT NULL DEFAULT FALSE,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tl_store (store_id),
    INDEX idx_tl_unit (storage_unit),
    INDEX idx_tl_breach (breach_detected),
    CONSTRAINT fk_tl_store FOREIGN KEY (store_id) REFERENCES stores (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Rider Shift Selfie & Biometric KYC Verification
CREATE TABLE IF NOT EXISTS rider_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_partner_id BIGINT NOT NULL,
    shift_date DATE NOT NULL,
    verification_type VARCHAR(40) NOT NULL DEFAULT 'FACIAL_SELFIE', -- FACIAL_SELFIE, ID_DOCUMENT, MANUAL_SUPERVISOR
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, MANUAL_REVIEW
    image_url TEXT,
    liveness_score DOUBLE DEFAULT 0.0,
    match_score DOUBLE DEFAULT 0.0,
    review_notes VARCHAR(255),
    verified_by_user_id BIGINT,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rv_partner (delivery_partner_id),
    INDEX idx_rv_date (shift_date),
    INDEX idx_rv_status (status),
    CONSTRAINT fk_rv_partner FOREIGN KEY (delivery_partner_id) REFERENCES delivery_partners (id) ON DELETE CASCADE,
    CONSTRAINT fk_rv_verifier FOREIGN KEY (verified_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Mobile Device Registrations for Push Notifications & Capacitor Client Tracking
CREATE TABLE IF NOT EXISTS mobile_device_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_token VARCHAR(255) NOT NULL,
    platform VARCHAR(20) NOT NULL DEFAULT 'ANDROID', -- ANDROID, IOS, WEB
    app_version VARCHAR(20) NOT NULL DEFAULT '3.0.0',
    device_model VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mdr_user (user_id),
    INDEX idx_mdr_token (device_token),
    CONSTRAINT fk_mdr_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
