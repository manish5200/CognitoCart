-- Add the is_deleted column to all primary entities safely
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE NOT NULL;

-- In case you have an active database, we need to ensure current rows don't crash
UPDATE users SET is_deleted = false WHERE is_deleted IS NULL;
UPDATE products SET is_deleted = false WHERE is_deleted IS NULL;
UPDATE categories SET is_deleted = false WHERE is_deleted IS NULL;
UPDATE coupons SET is_deleted = false WHERE is_deleted IS NULL;

-- Safely create Essential B-Tree Indexes
CREATE INDEX IF NOT EXISTS idx_users_is_deleted ON users(is_deleted);
CREATE INDEX IF NOT EXISTS idx_products_is_deleted ON products(is_deleted);
CREATE INDEX IF NOT EXISTS idx_categories_is_deleted ON categories(is_deleted);
CREATE INDEX IF NOT EXISTS idx_coupons_is_deleted ON coupons(is_deleted);
