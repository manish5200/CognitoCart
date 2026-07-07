-- =============================================================================
-- V35: Enterprise ID Hardening
-- Adds public_id (UUID) to all 23 entity tables, creates isolated per-table
-- sequences (21 real sequences — 2 skipped due to @MapsId on SellerProfile and
-- CustomerProfile which borrow their PK from Users), and adds human-readable
-- IDs to 7 customer-facing tables.
--
-- WHY NO CONCURRENTLY:
-- CREATE INDEX CONCURRENTLY waits for ALL open DB connections to finish their
-- current transactions before it begins. HikariCP opens a connection pool at
-- Spring Boot startup and those connections are always "open", causing
-- CONCURRENTLY to deadlock indefinitely. Since this migration runs at startup
-- with zero user traffic, a standard index build acquires a brief lock, runs
-- in milliseconds, and is the correct tool here. CONCURRENTLY is reserved for
-- live production systems with active read/write traffic on the indexed table.
-- =============================================================================

-- ─── SEQUENCES (21 per-table, start above any legacy IDENTITY data) ──────────
-- allocationSize=50 means Hibernate pre-fetches 50 IDs in one DB round-trip,
-- enabling true JDBC batch inserts. Each table gets its own isolated sequence so
-- IDs increment cleanly (1, 2, 3...) within a domain for BI/analytics queries.
--
-- SKIPPED — customer_profile_seq: CustomerProfile uses @MapsId, inheriting the
--   Users primary key. Hibernate never calls a sequence for this entity.
-- SKIPPED — seller_profile_seq:   SellerProfile uses @MapsId for the same reason.
CREATE SEQUENCE IF NOT EXISTS user_seq               START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS address_seq            START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS product_seq            START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS product_variant_seq    START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS category_seq           START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS product_insights_seq   START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS return_policy_seq      START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS review_seq             START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS order_seq              START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS order_item_seq         START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS shipment_seq           START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS cart_seq               START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS cart_item_seq          START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS wishlist_seq           START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS coupon_seq             START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS coupon_usage_seq       START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS sale_event_seq         START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS flash_sale_item_seq    START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS notification_seq       START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS failed_webhook_seq     START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS refresh_token_seq      START WITH 100000 INCREMENT BY 50;

-- ─── PUBLIC ID (UUID) — ALL 23 TABLES ────────────────────────────────────────
-- 3-step safe migration pattern per table:
--   Step 1: ADD as NULLABLE   — existing rows get NULL (no crash on ALTER)
--   Step 2: BACKFILL          — gen_random_uuid() is a native Postgres function,
--                               no Java round-trip needed, runs fully in the DB
--   Step 3: SET NOT NULL      — every row now has a value, constraint is safe
-- Index is built AFTER backfill so the planner can use statistics from day one.

ALTER TABLE users                 ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE users                      SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE users                 ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_public_id ON users(public_id);

ALTER TABLE customer_profiles     ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE customer_profiles          SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE customer_profiles     ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_profiles_public_id ON customer_profiles(public_id);

ALTER TABLE seller_profiles       ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE seller_profiles            SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE seller_profiles       ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_seller_profiles_public_id ON seller_profiles(public_id);

-- NOTE: Address entity maps to "user_addresses" (not "addresses") per V1__initial_schema.sql
ALTER TABLE user_addresses        ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE user_addresses             SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE user_addresses        ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_addresses_public_id ON user_addresses(public_id);

ALTER TABLE products              ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE products                   SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE products              ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_public_id ON products(public_id);

ALTER TABLE product_variants      ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE product_variants           SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE product_variants      ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_product_variants_public_id ON product_variants(public_id);

ALTER TABLE categories            ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE categories                 SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE categories            ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_public_id ON categories(public_id);

ALTER TABLE product_insights      ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE product_insights           SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE product_insights      ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_product_insights_public_id ON product_insights(public_id);

ALTER TABLE product_return_policy ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE product_return_policy      SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE product_return_policy ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_return_policy_public_id ON product_return_policy(public_id);

ALTER TABLE reviews               ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE reviews                    SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE reviews               ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_reviews_public_id ON reviews(public_id);

ALTER TABLE orders                ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE orders                     SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE orders                ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_public_id ON orders(public_id);

ALTER TABLE order_items           ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE order_items                SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE order_items           ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_order_items_public_id ON order_items(public_id);

ALTER TABLE shipments             ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE shipments                  SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE shipments             ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_shipments_public_id ON shipments(public_id);

ALTER TABLE carts                 ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE carts                      SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE carts                 ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_carts_public_id ON carts(public_id);

ALTER TABLE cart_items            ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE cart_items                 SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE cart_items            ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_cart_items_public_id ON cart_items(public_id);

-- NOTE: Wishlist entity maps to "user_wishlist" (not "wishlists") per V1__initial_schema.sql
ALTER TABLE user_wishlist         ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE user_wishlist              SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE user_wishlist         ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_wishlist_public_id ON user_wishlist(public_id);

ALTER TABLE coupons               ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE coupons                    SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE coupons               ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_coupons_public_id ON coupons(public_id);

ALTER TABLE user_coupon_usage     ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE user_coupon_usage          SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE user_coupon_usage     ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_coupon_usage_public_id ON user_coupon_usage(public_id);

ALTER TABLE platform_sale_events  ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE platform_sale_events       SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE platform_sale_events  ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_sale_events_public_id ON platform_sale_events(public_id);

ALTER TABLE flash_sale_items      ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE flash_sale_items           SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE flash_sale_items      ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_flash_sale_items_public_id ON flash_sale_items(public_id);

ALTER TABLE notifications         ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE notifications              SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE notifications         ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_notifications_public_id ON notifications(public_id);

ALTER TABLE failed_webhook_events ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE failed_webhook_events      SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE failed_webhook_events ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_failed_webhooks_public_id ON failed_webhook_events(public_id);

ALTER TABLE refresh_tokens        ADD COLUMN IF NOT EXISTS public_id UUID;
UPDATE refresh_tokens             SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE refresh_tokens        ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_public_id ON refresh_tokens(public_id);

-- ─── HUMAN IDs (7 customer-facing tables only) ────────────────────────────────
-- Backfill uses the ACTUAL creation date from the BaseEntity audit column (created_at).
-- Suffix uses zero-padded internal ID: guaranteed unique in SQL, no Java needed.
-- New records created via app will use HumanIdGenerator: PREFIX-YYYYMMDD-{6 random chars}
-- Legacy records get:                                    PREFIX-YYYYMMDD-{zero-padded id}
-- Example — old order from Jan 15: ORD-20260115-000005
-- Example — new order today:       ORD-20260703-K7P2MQ  (clearly post-migration format)

ALTER TABLE users                 ADD COLUMN IF NOT EXISTS account_number VARCHAR(30);
UPDATE users                      SET account_number = 'USR-' || TO_CHAR(created_at, 'YYYYMMDD') || '-' || LPAD(id::TEXT, 6, '0') WHERE account_number IS NULL;
ALTER TABLE users                 ALTER COLUMN account_number SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_account_number ON users(account_number);

ALTER TABLE seller_profiles       ADD COLUMN IF NOT EXISTS seller_code VARCHAR(30);
UPDATE seller_profiles            SET seller_code = 'SEL-' || TO_CHAR(created_at, 'YYYYMMDD') || '-' || LPAD(user_id::TEXT, 6, '0') WHERE seller_code IS NULL;
ALTER TABLE seller_profiles       ALTER COLUMN seller_code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_seller_profiles_seller_code ON seller_profiles(seller_code);

ALTER TABLE products              ADD COLUMN IF NOT EXISTS product_code VARCHAR(30);
UPDATE products                   SET product_code = 'PRD-' || TO_CHAR(created_at, 'YYYYMMDD') || '-' || LPAD(id::TEXT, 6, '0') WHERE product_code IS NULL;
ALTER TABLE products              ALTER COLUMN product_code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_product_code ON products(product_code);

ALTER TABLE orders                ADD COLUMN IF NOT EXISTS order_number VARCHAR(30);
UPDATE orders                     SET order_number = 'ORD-' || TO_CHAR(created_at, 'YYYYMMDD') || '-' || LPAD(id::TEXT, 6, '0') WHERE order_number IS NULL;
ALTER TABLE orders                ALTER COLUMN order_number SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);

ALTER TABLE shipments             ADD COLUMN IF NOT EXISTS tracking_code VARCHAR(30);
UPDATE shipments                  SET tracking_code = 'SHP-' || TO_CHAR(created_at, 'YYYYMMDD') || '-' || LPAD(id::TEXT, 6, '0') WHERE tracking_code IS NULL;
ALTER TABLE shipments             ALTER COLUMN tracking_code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_shipments_tracking_code ON shipments(tracking_code);

ALTER TABLE platform_sale_events  ADD COLUMN IF NOT EXISTS event_code VARCHAR(30);
UPDATE platform_sale_events       SET event_code = 'EVT-' || TO_CHAR(created_at, 'YYYYMMDD') || '-' || LPAD(id::TEXT, 6, '0') WHERE event_code IS NULL;
ALTER TABLE platform_sale_events  ALTER COLUMN event_code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_sale_events_event_code ON platform_sale_events(event_code);
