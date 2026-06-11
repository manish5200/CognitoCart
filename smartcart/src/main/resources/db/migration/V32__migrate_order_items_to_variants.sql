-- ============================================================================
-- V32: MIGRATE order_items FROM product_id → variant_id + frozen snapshots
-- ============================================================================
-- ORDER OF OPERATIONS (must not be changed):
--   1. Add new columns as nullable (DB cannot add NOT NULL without data)
--   2. Back-fill data from existing products + auto-created variants (from V30)
--   3. Enforce NOT NULL + FK constraints (now safe — all rows have values)
--   4. Drop old product_id column
--   5. Add missing audit columns order_items was missing from V1 schema
--   6. Add performance indexes
-- ============================================================================

-- STEP 1: Add all new columns (nullable during migration window)
ALTER TABLE order_items
    ADD COLUMN variant_id             BIGINT,
    ADD COLUMN line_total             DECIMAL(10, 2),
    ADD COLUMN product_name_snapshot  VARCHAR(255),
    ADD COLUMN variant_label_snapshot VARCHAR(100),
    ADD COLUMN sku_snapshot           VARCHAR(100);


-- STEP 2: Populate variant_id for all existing order items.
-- V30 auto-created one "Standard" variant for every product.
-- We join through that to map historical order items to their default variant.
UPDATE order_items oi
SET variant_id = pv.id
    FROM product_variants pv
WHERE pv.product_id = oi.product_id
  AND pv.attributes = '{"Type":"Standard"}'::jsonb;


-- STEP 3: Populate frozen snapshot columns from existing product data.
-- These become the permanent, uneditable historical record for all existing orders.
-- 'Standard' is used as variantLabel since all legacy products had no variants.
UPDATE order_items oi
SET
    product_name_snapshot  = p.product_name,
    variant_label_snapshot = 'Standard',
    sku_snapshot           = pv.sku,
    line_total             = COALESCE(oi.price_at_purchase, 0.00)
        * COALESCE(oi.quantity, 1)
    FROM products p
JOIN product_variants pv
ON pv.product_id = p.id
    AND pv.attributes = '{"Type":"Standard"}'::jsonb
WHERE oi.product_id = p.id;


-- STEP 4: Add missing audit columns (order_items was missing these from V1 schema)
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMP DEFAULT NOW() NOT NULL,
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS modified_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS version     BIGINT DEFAULT 0;


-- STEP 5: Enforce constraints now that all rows are populated
-- variant_id stays NULLABLE intentionally — see entity javadoc above
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_item_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id);

-- These snapshot columns are mandatory on all new orders going forward
ALTER TABLE order_items
    ALTER COLUMN line_total            SET NOT NULL,
ALTER COLUMN product_name_snapshot SET NOT NULL;


-- STEP 6: Drop the old product_id column
-- All display data now comes from snapshot columns.
-- All live operations go through variant_id → product_variants → products chain.
ALTER TABLE order_items
DROP COLUMN IF EXISTS product_id;


-- STEP 7: Performance indexes
CREATE INDEX IF NOT EXISTS idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_variant_id ON order_items(variant_id);
