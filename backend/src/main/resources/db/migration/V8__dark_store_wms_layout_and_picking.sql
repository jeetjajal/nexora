-- =============================================================================
-- NEXORA QUICK COMMERCE PLATFORM - V8 PHASE 3 DARK STORE WMS SCHEMA
-- Dark Store Layout (Zones, Aisles, Racks, Shelves, Bins),
-- Product Storage Mappings, Barcodes, Pick Tasks, Picklists,
-- S-Shape Pick Paths, Packing QC, and Dispatch Handovers.
-- =============================================================================

-- 1. Dark Store Zones (Ambient, Chilled, Frozen, High Value)
CREATE TABLE IF NOT EXISTS store_zones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    zone_code VARCHAR(30) NOT NULL, -- ZONE-AMBIENT, ZONE-COLD, ZONE-FROZEN, ZONE-HIGH-VALUE
    name VARCHAR(100) NOT NULL,
    temperature_type VARCHAR(30) NOT NULL DEFAULT 'AMBIENT', -- AMBIENT, CHILLED, FROZEN
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_zone (store_id, zone_code),
    INDEX idx_sz_store (store_id),
    CONSTRAINT fk_sz_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Store Aisles (Physical corridors in the warehouse)
CREATE TABLE IF NOT EXISTS store_aisles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    aisle_code VARCHAR(20) NOT NULL, -- A01, A02, A03...
    aisle_number INT NOT NULL, -- 1, 2, 3... used for S-Shape sequence ordering
    direction VARCHAR(20) NOT NULL DEFAULT 'BIDIRECTIONAL', -- BIDIRECTIONAL, ONE_WAY
    is_restricted BOOLEAN NOT NULL DEFAULT FALSE,
    width_meters DECIMAL(4, 2) NOT NULL DEFAULT 1.80,
    length_meters DECIMAL(5, 2) NOT NULL DEFAULT 12.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_aisle (store_id, aisle_code),
    INDEX idx_sa_zone (zone_id),
    INDEX idx_sa_store (store_id),
    CONSTRAINT fk_sa_zone FOREIGN KEY (zone_id) REFERENCES store_zones (id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Store Racks (Shelving units lining each aisle)
CREATE TABLE IF NOT EXISTS store_racks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aisle_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    rack_code VARCHAR(20) NOT NULL, -- R01, R02...
    rack_number INT NOT NULL, -- Position index along the aisle
    side VARCHAR(10) NOT NULL DEFAULT 'LEFT', -- LEFT, RIGHT
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_aisle_rack (aisle_id, rack_code),
    INDEX idx_sr_aisle (aisle_id),
    INDEX idx_sr_store (store_id),
    CONSTRAINT fk_sr_aisle FOREIGN KEY (aisle_id) REFERENCES store_aisles (id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Store Shelves (Vertical levels on each rack)
CREATE TABLE IF NOT EXISTS store_shelves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rack_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    shelf_code VARCHAR(20) NOT NULL, -- S01, S02, S03...
    shelf_level INT NOT NULL DEFAULT 1, -- 1 (bottom), 2 (middle), 3 (eye-level), 4 (top)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rack_shelf (rack_id, shelf_code),
    INDEX idx_ss_rack (rack_id),
    INDEX idx_ss_store (store_id),
    CONSTRAINT fk_ss_rack FOREIGN KEY (rack_id) REFERENCES store_racks (id) ON DELETE CASCADE,
    CONSTRAINT fk_ss_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Store Bins (Exact physical storage locations for SKUs)
CREATE TABLE IF NOT EXISTS store_bins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shelf_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    bin_code VARCHAR(20) NOT NULL, -- B01, B02...
    full_location_code VARCHAR(80) NOT NULL UNIQUE, -- e.g. ST04-ZAMB-A01-R02-S03-B01
    traversal_sequence INT NOT NULL DEFAULT 0, -- Pre-sorted S-Shape traversal index
    x_coord DECIMAL(6, 2) NOT NULL DEFAULT 0.00, -- 2D warehouse X coordinate (meters)
    y_coord DECIMAL(6, 2) NOT NULL DEFAULT 0.00, -- 2D warehouse Y coordinate (meters)
    capacity_units INT NOT NULL DEFAULT 100,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, MAINTENANCE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sb_shelf (shelf_id),
    INDEX idx_sb_store (store_id),
    INDEX idx_sb_traversal (store_id, traversal_sequence),
    CONSTRAINT fk_sb_shelf FOREIGN KEY (shelf_id) REFERENCES store_shelves (id) ON DELETE CASCADE,
    CONSTRAINT fk_sb_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Product Storage Locations (Many-to-Many mapping with primary picking bin flag)
CREATE TABLE IF NOT EXISTS product_storage_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    bin_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    max_capacity INT NOT NULL DEFAULT 50,
    current_stock INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prod_bin (product_id, bin_id),
    INDEX idx_psl_prod (product_id),
    INDEX idx_psl_bin (bin_id),
    INDEX idx_psl_store (store_id),
    CONSTRAINT fk_psl_prod FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_psl_bin FOREIGN KEY (bin_id) REFERENCES store_bins (id) ON DELETE CASCADE,
    CONSTRAINT fk_psl_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Product Barcodes (EAN-13, UPC, SKU, QR mapping for scanner resolution)
CREATE TABLE IF NOT EXISTS product_barcodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    barcode_value VARCHAR(64) NOT NULL UNIQUE,
    barcode_type VARCHAR(20) NOT NULL DEFAULT 'EAN_13', -- EAN_13, UPC_A, QR_CODE, INTERNAL_SKU
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pb_product (product_id),
    INDEX idx_pb_value (barcode_value),
    CONSTRAINT fk_pb_prod FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add SKU and primary barcode columns to products table if missing
ALTER TABLE products ADD COLUMN IF NOT EXISTS sku VARCHAR(50) NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS primary_barcode VARCHAR(64) NULL;
CREATE INDEX idx_products_sku ON products (sku);
CREATE INDEX idx_products_barcode ON products (primary_barcode);

-- 8. Picker Profiles (Operational shift and status tracking)
CREATE TABLE IF NOT EXISTS picker_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    store_id BIGINT NOT NULL,
    employee_code VARCHAR(30) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, BUSY, ON_BREAK, OFFLINE
    current_task_id BIGINT NULL,
    completed_tasks_count INT NOT NULL DEFAULT 0,
    average_pick_seconds_per_item DECIMAL(5, 2) NOT NULL DEFAULT 8.00,
    shift_started_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pp_user (user_id),
    INDEX idx_pp_store (store_id),
    INDEX idx_pp_status (status),
    CONSTRAINT fk_pp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_pp_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Pick Tasks (WMS picklist container with path optimization metadata)
CREATE TABLE IF NOT EXISTS pick_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_code VARCHAR(64) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL UNIQUE,
    store_id BIGINT NOT NULL,
    picker_id BIGINT NULL, -- references picker_profiles(id)
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, ASSIGNED, IN_PROGRESS, PARTIALLY_PICKED, COMPLETED, EXCEPTION, CANCELLED
    total_items_count INT NOT NULL DEFAULT 0,
    picked_items_count INT NOT NULL DEFAULT 0,
    estimated_distance_meters DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    estimated_time_seconds INT NOT NULL DEFAULT 0,
    selected_algorithm VARCHAR(30) NOT NULL DEFAULT 'S_SHAPE', -- S_SHAPE, NEAREST_NEIGHBOR
    assigned_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    exception_reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pt_order (order_id),
    INDEX idx_pt_store (store_id),
    INDEX idx_pt_picker (picker_id),
    INDEX idx_pt_status (status),
    CONSTRAINT fk_pt_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_pt_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE,
    CONSTRAINT fk_pt_picker FOREIGN KEY (picker_id) REFERENCES picker_profiles (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Pick Task Items (Ordered picklist lines sequenced by S-Shape engine)
CREATE TABLE IF NOT EXISTS pick_task_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pick_task_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    bin_id BIGINT NULL,
    batch_id BIGINT NULL, -- FEFO reserved batch reference
    sequence_index INT NOT NULL DEFAULT 1, -- S-Shape ordered visit sequence
    required_quantity INT NOT NULL DEFAULT 1,
    picked_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, PICKED, SHORTAGE, SUBSTITUTED
    scanned_barcode VARCHAR(64) NULL,
    picked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pti_task (pick_task_id),
    INDEX idx_pti_order_item (order_item_id),
    INDEX idx_pti_prod (product_id),
    INDEX idx_pti_bin (bin_id),
    INDEX idx_pti_batch (batch_id),
    CONSTRAINT fk_pti_task FOREIGN KEY (pick_task_id) REFERENCES pick_tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_pti_oi FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_pti_prod FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_pti_bin FOREIGN KEY (bin_id) REFERENCES store_bins (id) ON DELETE SET NULL,
    CONSTRAINT fk_pti_batch FOREIGN KEY (batch_id) REFERENCES inventory_batches (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Picking Events (Immutable audit log for scanner events and exception tracking)
CREATE TABLE IF NOT EXISTS picking_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pick_task_id BIGINT NOT NULL,
    pick_task_item_id BIGINT NULL,
    operator_id BIGINT NOT NULL,
    scanned_raw_value VARCHAR(100) NOT NULL,
    event_type VARCHAR(30) NOT NULL, -- SCAN_SUCCESS, WRONG_PRODUCT, WRONG_STORE, EXPIRED_BATCH, EXCESS_QUANTITY, UNKNOWN_BARCODE, SHORTAGE_REPORTED
    is_success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pe_task (pick_task_id),
    INDEX idx_pe_operator (operator_id),
    CONSTRAINT fk_pe_task FOREIGN KEY (pick_task_id) REFERENCES pick_tasks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Packing Tasks (QC and Bag Sealing workflow)
CREATE TABLE IF NOT EXISTS packing_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    store_id BIGINT NOT NULL,
    packer_id BIGINT NULL,
    bag_code VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, PACKING, PACKED, HANDOVER_READY
    qc_passed BOOLEAN NOT NULL DEFAULT TRUE,
    missing_items_count INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pkt_order (order_id),
    INDEX idx_pkt_store (store_id),
    CONSTRAINT fk_pkt_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_pkt_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Dispatch Handovers (Rider Handover Audit linking WMS to Delivery Engine)
CREATE TABLE IF NOT EXISTS dispatch_handovers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    store_id BIGINT NOT NULL,
    delivery_partner_id BIGINT NOT NULL,
    bag_code VARCHAR(64) NOT NULL,
    handover_by_user_id BIGINT NOT NULL,
    handover_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dh_order (order_id),
    INDEX idx_dh_partner (delivery_partner_id),
    CONSTRAINT fk_dh_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_dh_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE,
    CONSTRAINT fk_dh_partner FOREIGN KEY (delivery_partner_id) REFERENCES delivery_partners (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Seed Initial WMS Layout for Store #4 (Nexora Dark Store SG Highway) if store exists
INSERT IGNORE INTO store_zones (store_id, zone_code, name, temperature_type)
SELECT 4, 'ZONE-AMBIENT', 'Ambient Grocery & FMCG Zone', 'AMBIENT' FROM stores WHERE id = 4
UNION ALL
SELECT 4, 'ZONE-COLD', 'Dairy & Fresh Produce Chilled Zone', 'CHILLED' FROM stores WHERE id = 4
UNION ALL
SELECT 4, 'ZONE-FROZEN', 'Frozen & Ice Creams Zone', 'FROZEN' FROM stores WHERE id = 4;

-- Aisles for Zone Ambient (A01, A02, A03)
INSERT IGNORE INTO store_aisles (zone_id, store_id, aisle_code, aisle_number, width_meters, length_meters)
SELECT id, 4, 'A01', 1, 1.80, 15.00 FROM store_zones WHERE store_id = 4 AND zone_code = 'ZONE-AMBIENT';
INSERT IGNORE INTO store_aisles (zone_id, store_id, aisle_code, aisle_number, width_meters, length_meters)
SELECT id, 4, 'A02', 2, 1.80, 15.00 FROM store_zones WHERE store_id = 4 AND zone_code = 'ZONE-AMBIENT';
INSERT IGNORE INTO store_aisles (zone_id, store_id, aisle_code, aisle_number, width_meters, length_meters)
SELECT id, 4, 'A03', 3, 1.80, 15.00 FROM store_zones WHERE store_id = 4 AND zone_code = 'ZONE-AMBIENT';

-- Aisles for Zone Cold (A04)
INSERT IGNORE INTO store_aisles (zone_id, store_id, aisle_code, aisle_number, width_meters, length_meters)
SELECT id, 4, 'A04', 4, 2.00, 10.00 FROM store_zones WHERE store_id = 4 AND zone_code = 'ZONE-COLD';

-- Racks for A01 (R01, R02)
INSERT IGNORE INTO store_racks (aisle_id, store_id, rack_code, rack_number, side)
SELECT id, 4, 'R01', 1, 'LEFT' FROM store_aisles WHERE store_id = 4 AND aisle_code = 'A01';
INSERT IGNORE INTO store_racks (aisle_id, store_id, rack_code, rack_number, side)
SELECT id, 4, 'R02', 2, 'RIGHT' FROM store_aisles WHERE store_id = 4 AND aisle_code = 'A01';

-- Shelves for R01 (S01, S02, S03)
INSERT IGNORE INTO store_shelves (rack_id, store_id, shelf_code, shelf_level)
SELECT id, 4, 'S01', 1 FROM store_racks WHERE store_id = 4 AND rack_code = 'R01';
INSERT IGNORE INTO store_shelves (rack_id, store_id, shelf_code, shelf_level)
SELECT id, 4, 'S02', 2 FROM store_racks WHERE store_id = 4 AND rack_code = 'R01';
INSERT IGNORE INTO store_shelves (rack_id, store_id, shelf_code, shelf_level)
SELECT id, 4, 'S03', 3 FROM store_racks WHERE store_id = 4 AND rack_code = 'R01';

-- Bins for S01, S02, S03 with coordinates & traversal sequence
INSERT IGNORE INTO store_bins (shelf_id, store_id, bin_code, full_location_code, traversal_sequence, x_coord, y_coord)
SELECT s.id, 4, 'B01', 'ST04-ZAMB-A01-R01-S01-B01', 10, 2.00, 2.50
FROM store_shelves s JOIN store_racks r ON s.rack_id = r.id WHERE r.rack_code = 'R01' AND s.shelf_code = 'S01' AND r.store_id = 4;

INSERT IGNORE INTO store_bins (shelf_id, store_id, bin_code, full_location_code, traversal_sequence, x_coord, y_coord)
SELECT s.id, 4, 'B02', 'ST04-ZAMB-A01-R01-S02-B01', 20, 2.00, 5.00
FROM store_shelves s JOIN store_racks r ON s.rack_id = r.id WHERE r.rack_code = 'R01' AND s.shelf_code = 'S02' AND r.store_id = 4;

INSERT IGNORE INTO store_bins (shelf_id, store_id, bin_code, full_location_code, traversal_sequence, x_coord, y_coord)
SELECT s.id, 4, 'B03', 'ST04-ZAMB-A01-R01-S03-B01', 30, 2.00, 8.50
FROM store_shelves s JOIN store_racks r ON s.rack_id = r.id WHERE r.rack_code = 'R01' AND s.shelf_code = 'S03' AND r.store_id = 4;
