-- ============================================================================
-- V30: PRODUCT VARIANTS SYSTEM
-- ============================================================================
-- Architecture shift: Stock and SKU move from the Product (catalog layer)
-- to the ProductVariant (inventory layer).
--
-- WHY THIS ORDER MATTERS:
--   1. Create table first (nothing depends on it yet)
--   2. Migrate existing data (so no product is left "variant-less")
--   3. Drop old columns last (after data is safely copied — no rollback risk)
-- ============================================================================


-- ─── STEP 1: CREATE THE product_variants TABLE ───────────────────────────────

CREATE TABLE product_variants (
                                  id                  BIGSERIAL PRIMARY KEY,

    -- FK to master product — the catalog shell this variant belongs to
                                  product_id          BIGINT NOT NULL REFERENCES products(id),

    -- Internal warehouse identifier (seller-defined, globally unique)
                                  sku                 VARCHAR(100) NOT NULL UNIQUE,

    -- Global retail barcode: EAN-13 / UPC-A / ISBN-13 (nullable for small sellers)
                                  barcode             VARCHAR(50) UNIQUE,

    -- JSONB: flexible key-value attributes for ANY product type.
    -- No schema change ever needed when adding new product categories.
    -- T-Shirt: {"Size":"L","Color":"Red"} | Laptop: {"RAM":"16GB","Storage":"512GB"}
                                  attributes          JSONB NOT NULL DEFAULT '{"Type":"Standard"}',

    -- ─── STOCK ───
                                  stock_quantity      INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),

    -- Cart reservations prevent overselling before checkout completes.
    -- availableStock = stock_quantity - reserved_quantity
                                  reserved_quantity   INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),

    -- Seller-defined threshold: trigger low-stock alert before hitting zero
                                  low_stock_threshold INTEGER NOT NULL DEFAULT 5,

    -- ─── PRICING ───
    -- Delta from master product base price (positive = more expensive variant)
                                  price_modifier      DECIMAL(10, 2) NOT NULL DEFAULT 0.00,

    -- "Was ₹2000" crossed-out price for this specific variant (Shopify: compareAtPrice)
    -- NULL = no variant-level sale active; fall back to product.discount_price
                                  compare_at_price    DECIMAL(10, 2),

    -- Cost of Goods Sold — supplier cost. NEVER exposed to customers.
    -- Used exclusively for backend P&L margin analytics.
                                  cost_price          DECIMAL(10, 2),

    -- ─── LOGISTICS ───
                                  weight              DECIMAL(6, 3),       -- actual weight in kg
                                  length_cm           DECIMAL(6, 2),       -- for volumetric weight: (L×W×H)/5000
                                  width_cm            DECIMAL(6, 2),
                                  height_cm           DECIMAL(6, 2),

    -- ─── DISPLAY ───
    -- Variant-specific thumbnail (e.g., red shirt image when "Red" is selected).
    -- NULL = fall back to products.imageUrls gallery
                                  variant_image_url   VARCHAR(512),

    -- Controls UI display order of variant buttons (S=0, M=1, L=2, XL=3)
    -- Without this: random insertion order causes S/XL/M chaos in the UI
                                  sort_order          INTEGER NOT NULL DEFAULT 0,

    -- ─── LIFECYCLE ───
                                  is_active           BOOLEAN NOT NULL DEFAULT TRUE,

    -- ─── BASE ENTITY AUDIT COLUMNS (matches BaseEntity.java) ───
                                  created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                  updated_at          TIMESTAMP,
                                  created_by          VARCHAR(255),
                                  modified_by         VARCHAR(255),
                                  version             BIGINT DEFAULT 0
);


-- ─── STEP 2: MIGRATE EXISTING PRODUCT DATA ───────────────────────────────────
-- Every existing product currently has sku + stock_quantity sitting directly
-- on the products table. We create one "Default" variant per product,
-- copying those values across. Zero data loss.
--
-- COALESCE(sku, 'DEFAULT-' || id):
--   If a product somehow has a NULL sku, we generate a fallback
--   so the NOT NULL constraint never fires during migration.

INSERT INTO product_variants (
    product_id,
    sku,
    attributes,
    stock_quantity,
    reserved_quantity,
    low_stock_threshold,
    price_modifier,
    sort_order,
    is_active,
    created_at,
    version
)
SELECT
    id                                          AS product_id,
    COALESCE(sku, 'DEFAULT-' || id::TEXT)       AS sku,
    '{"Type":"Standard"}'::jsonb                AS attributes,
    COALESCE(stock_quantity, 0)                 AS stock_quantity,
    0                                           AS reserved_quantity,
    5                                           AS low_stock_threshold,
    0.00                                        AS price_modifier,
    0                                           AS sort_order,
    TRUE                                        AS is_active,
    NOW()                                       AS created_at,
    0                                           AS version
FROM products;


-- ─── STEP 3: ADD NEW COLUMNS TO products TABLE ───────────────────────────────
-- These were in the updated Product.java entity but not in the original schema.

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS brand       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS total_sold  INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discount_price DECIMAL(10, 2);


-- ─── STEP 4: REMOVE COLUMNS THAT MOVED TO product_variants ───────────────────
-- Now that all SKUs and stock counts are safely in product_variants,
-- keeping them on products would create a dual source of truth —
-- the #1 cause of inventory synchronization bugs at scale.
-- Dropping them here makes the data contract explicit and unambiguous.

ALTER TABLE products
DROP COLUMN IF EXISTS sku,
    DROP COLUMN IF EXISTS stock_quantity;


-- ─── STEP 5: PERFORMANCE INDEXES ─────────────────────────────────────────────

-- Fast lookup: "get all variants of product X" (product page API)
CREATE INDEX idx_variant_product_id  ON product_variants(product_id);

-- Fast lookup: "show only active variants" (customer-facing catalog)
CREATE INDEX idx_variant_active      ON product_variants(is_active);

-- JSONB index: supports queries like WHERE attributes->>'RAM' = '16GB'
-- GIN (Generalized Inverted Index) is PostgreSQL's recommended index for JSONB
CREATE INDEX idx_variant_attributes  ON product_variants USING GIN (attributes);

-- Featured products homepage query: WHERE is_featured = TRUE AND is_deleted = FALSE
CREATE INDEX idx_products_featured   ON products(is_featured) WHERE is_featured = TRUE;
