-- 1. Upgrade the coupons table safely
DO $$
BEGIN
    BEGIN
        ALTER TABLE coupons ADD COLUMN discount_type VARCHAR(20) DEFAULT 'PERCENTAGE';
    EXCEPTION WHEN duplicate_column THEN NULL;
    END;
    BEGIN
        ALTER TABLE coupons ADD COLUMN discount_value DECIMAL(10, 2) DEFAULT 0.00;
    EXCEPTION WHEN duplicate_column THEN NULL;
    END;
    BEGIN
        ALTER TABLE coupons ADD COLUMN min_order_amount DECIMAL(10, 2);
    EXCEPTION WHEN duplicate_column THEN NULL;
    END;
    BEGIN
        ALTER TABLE coupons ADD COLUMN max_discount_amount DECIMAL(10, 2);
    EXCEPTION WHEN duplicate_column THEN NULL;
    END;
    BEGIN
        ALTER TABLE coupons ADD COLUMN valid_from TIMESTAMP;
    EXCEPTION WHEN duplicate_column THEN NULL;
    END;
    BEGIN
        ALTER TABLE coupons ADD COLUMN max_uses_per_user INTEGER;
    EXCEPTION WHEN duplicate_column THEN NULL;
    END;
    BEGIN
        ALTER TABLE coupons ADD COLUMN is_first_order_only BOOLEAN DEFAULT FALSE;
    EXCEPTION WHEN duplicate_column THEN NULL;
    END;
END $$;

-- Drop the old column safely
DO $$
BEGIN
    ALTER TABLE coupons DROP COLUMN discount_percentage;
EXCEPTION WHEN undefined_column THEN NULL;
END $$;

-- 2. Add delivery fee tracking to Carts and Orders safely
DO $$
BEGIN
    BEGIN
        ALTER TABLE carts ADD COLUMN delivery_fee DECIMAL(10, 2) DEFAULT 0.00;
    EXCEPTION WHEN duplicate_column THEN NULL;
    END;
    BEGIN
        ALTER TABLE orders ADD COLUMN delivery_fee DECIMAL(10, 2) DEFAULT 0.00;
    EXCEPTION WHEN duplicate_column THEN NULL;
    END;
END $$;

-- 3. Create a table to track Per-User Coupon Usage safely
CREATE TABLE IF NOT EXISTS user_coupon_usage (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    usage INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usage_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_usage_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT unique_user_coupon UNIQUE (user_id, coupon_id)
);

-- Add index safely (bypassing ownership errors if created by postgres superuser manually)
DO $$
BEGIN
    CREATE INDEX idx_user_coupon ON user_coupon_usage(user_id, coupon_id);
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;
