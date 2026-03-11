-- ============================================================
-- V4: Performance Indexes — CognitoCart
-- ============================================================
-- These indexes target the exact columns hit by JPA/JPQL
-- queries in ProductRepository, OrderRepository etc.
-- Without these, PostgreSQL does a full table scan on every
-- request; with them, lookups are O(log n) via B-Tree.
-- ============================================================

-- ============================================================
-- PRODUCTS
-- ============================================================
-- Slug is used for SEO product detail pages (getProductBySlug)
CREATE INDEX IF NOT EXISTS idx_products_slug
    ON products(slug);

-- Category + soft-delete combo — getProductsByCategoryIds
CREATE INDEX IF NOT EXISTS idx_products_category_id
    ON products(category_id);

-- Seller sees only their own products (seller dashboard)
CREATE INDEX IF NOT EXISTS idx_products_seller_id
    ON products(seller_id);

-- Availability filter in product listings
CREATE INDEX IF NOT EXISTS idx_products_is_available
    ON products(is_available)
    WHERE is_deleted = FALSE;

-- Composite: soft-delete guard on all product queries
CREATE INDEX IF NOT EXISTS idx_products_active
    ON products(is_deleted, is_available);

-- ============================================================
-- CATEGORIES
-- ============================================================
-- Recursive tree traversal — getAllChildCategoryIds
CREATE INDEX IF NOT EXISTS idx_categories_parent_id
    ON categories(parent_id);

-- Slug-based lookups
CREATE INDEX IF NOT EXISTS idx_categories_slug
    ON categories(slug);

-- ============================================================
-- ORDERS
-- ============================================================
-- Customer order history — findByUserId
CREATE INDEX IF NOT EXISTS idx_orders_user_id
    ON orders(user_id);

-- Admin filters by status (e.g. all PENDING orders)
CREATE INDEX IF NOT EXISTS idx_orders_status
    ON orders(order_status);

-- Composite: customer's orders by status
CREATE INDEX IF NOT EXISTS idx_orders_user_status
    ON orders(user_id, order_status);

-- ============================================================
-- ORDER ITEMS
-- ============================================================
-- Join from order to its items
CREATE INDEX IF NOT EXISTS idx_order_items_order_id
    ON order_items(order_id);

-- Seller revenue queries — which items had their products
CREATE INDEX IF NOT EXISTS idx_order_items_product_id
    ON order_items(product_id);

-- ============================================================
-- CART & CART ITEMS
-- ============================================================
-- findByUserId (one cart per user, but still indexed for joins)
CREATE INDEX IF NOT EXISTS idx_carts_user_id
    ON carts(user_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id
    ON cart_items(cart_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_product_id
    ON cart_items(product_id);

-- ============================================================
-- REVIEWS
-- ============================================================
-- All reviews for a product
CREATE INDEX IF NOT EXISTS idx_reviews_product_id
    ON reviews(product_id);

-- All reviews by a user (prevent duplicate review check)
CREATE INDEX IF NOT EXISTS idx_reviews_user_id
    ON reviews(user_id);

-- ============================================================
-- ADDRESSES
-- ============================================================
-- User's address list
CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id
    ON user_addresses(user_id);

-- Default address lookup
CREATE INDEX IF NOT EXISTS idx_user_addresses_user_default
    ON user_addresses(user_id, is_default)
    WHERE is_deleted = FALSE;

-- ============================================================
-- WISHLIST
-- ============================================================
-- User's wishlist
CREATE INDEX IF NOT EXISTS idx_wishlist_user_id
    ON user_wishlist(user_id);

-- Composite: prevents duplicate wishlist entries, fast lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_wishlist_user_product
    ON user_wishlist(user_id, product_id);
