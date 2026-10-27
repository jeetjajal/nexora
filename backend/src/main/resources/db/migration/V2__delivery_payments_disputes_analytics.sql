-- =============================================================================
-- NEXORA QUICK COMMERCE PLATFORM - V2 DELIVERY, PAYMENTS, DISPUTES & NOTIFICATIONS
-- =============================================================================

CREATE TABLE IF NOT EXISTS delivery_partners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    vehicle_type VARCHAR(50) NOT NULL DEFAULT 'ELECTRIC_SCOOTER',
    vehicle_number VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ONLINE',
    current_latitude DOUBLE,
    current_longitude DOUBLE,
    last_ping_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_dp_status (status),
    CONSTRAINT fk_dp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS delivery_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ASSIGNED',
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    picked_up_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    INDEX idx_da_order (order_id),
    INDEX idx_da_partner (partner_id),
    CONSTRAINT fk_da_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_da_partner FOREIGN KEY (partner_id) REFERENCES delivery_partners (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    gateway VARCHAR(50) NOT NULL DEFAULT 'RAZORPAY',
    gateway_order_id VARCHAR(100),
    gateway_payment_id VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payments_order (order_id),
    INDEX idx_payments_gw_order (gateway_order_id),
    CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100),
    amount DECIMAL(10, 2) NOT NULL,
    raw_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pt_payment (payment_id),
    CONSTRAINT fk_pt_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS disputes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    customer_notes TEXT,
    claim_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    decision_notes TEXT,
    handled_by VARCHAR(100),
    refund_transaction_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    INDEX idx_disp_order (order_id),
    INDEX idx_disp_customer (customer_id),
    INDEX idx_disp_status (status),
    CONSTRAINT fk_disp_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_disp_customer FOREIGN KEY (customer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS surge_pricing_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hub_id BIGINT NOT NULL UNIQUE,
    hub_name VARCHAR(150),
    zone_name VARCHAR(150),
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    multiplier DECIMAL(4, 2) NOT NULL DEFAULT 1.00,
    extra_flat_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    reason VARCHAR(255),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_surge_hub (hub_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    recipient VARCHAR(150) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    event_type VARCHAR(48) NOT NULL,
    title VARCHAR(255) NOT NULL,
    sanitized_body TEXT NOT NULL,
    delivery_status VARCHAR(32) NOT NULL,
    provider_response VARCHAR(512),
    deduplication_key VARCHAR(128) UNIQUE,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    INDEX idx_notif_dedup (deduplication_key),
    INDEX idx_notif_order (order_id),
    INDEX idx_notif_recipient (recipient)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    scope VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    fingerprint VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_idem_key (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
