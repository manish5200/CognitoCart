-- ============================================================================
-- V31: MIGRATE cart_items FROM product_id → variant_id
-- ============================================================================
-- Cart items must reference the specific purchasable variant, not the generic
-- product. This enables precise stock deduction and variant-aware checkout.
--
-- MIGRATION STRATEGY (safe 4-step approach):
--   1. Add variant_id column as NULLABLE first (can't be NOT NULL yet — no data)
--   2. Populate: map each cart_item's product to its default variant
--   3. Enforce NOT NULL + FK constraint (now safe — all rows have a value)
--   4. Drop the old product_id column (no longer the source of truth)
-- ============================================================================


-- STEP 1: Add variant_id column (nullable during migration window)
ALTER TABLE cart_items
    ADD COLUMN variant_id BIGINT;


-- STEP 2: Populate variant_id for all existing cart items.
-- Each existing cart_item has a product_id. We find the "Default" variant
-- for that product (the one auto-created in V30 with attributes = Standard).
-- There is exactly one default variant per product from the V30 migration.
UPDATE cart_items ci
SET variant_id = pv.id
    FROM product_variants pv
WHERE pv.product_id = ci.product_id
  AND pv.attributes = '{"Type":"Standard"}'::jsonb;


-- STEP 3: Now that all rows have a variant_id, enforce constraints.
-- NOT NULL: every cart item MUST reference a variant going forward.
-- FK: ensures referential integrity — can't add a cart item for a deleted variant.
ALTER TABLE cart_items
    ALTER COLUMN variant_id SET NOT NULL,
    ADD CONSTRAINT fk_cart_item_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id);


-- STEP 4: Remove the old product_id column.
-- product is now reachable through: cart_item → variant → product
-- Keeping product_id would create a dual source of truth and invite sync bugs.
ALTER TABLE cart_items
DROP COLUMN IF EXISTS product_id;


-- STEP 5: Index for "is this variant already in this cart?" — dedup check
-- Runs on every "Add to Cart" call to prevent duplicate line items
CREATE INDEX idx_cart_items_variant_id ON cart_items(variant_id);
