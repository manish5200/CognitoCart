-- V5__add_coupon_system.sql
-- Create the coupons table
CREATE TABLE IF NOT EXISTS coupons (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    code VARCHAR(50) NOT NULL UNIQUE,
    discount_percentage NUMERIC(5,2) NOT NULL,
    max_uses INTEGER,
    current_uses INTEGER DEFAULT 0,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- Add coupon tracking fields to carts
ALTER TABLE carts
ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(10,2) DEFAULT 0.00;

-- Add coupon tracking fields to orders
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(10,2) DEFAULT 0.00;
