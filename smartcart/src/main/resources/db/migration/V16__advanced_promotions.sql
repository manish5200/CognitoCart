-- Advanced E-Commerce Promotion Conditions
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS applicable_category_id BIGINT;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS applicable_product_id BIGINT;

-- Buy-One-Get-One (BOGO) Support
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS buy_x_quantity INT;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS get_y_quantity INT;

-- Auto-Applied Magic Rules & Exclusive Targeting
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS is_auto_applied BOOLEAN DEFAULT FALSE;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS target_user_id BIGINT;

-- The Enterprise "Marketing Budget" Safeguard (Your amazing addition)
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS global_budget_limit NUMERIC(15, 2);
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS current_budget_used NUMERIC(15, 2) DEFAULT 0.00;
