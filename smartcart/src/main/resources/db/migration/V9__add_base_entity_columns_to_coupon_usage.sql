-- V9: Add missing BaseEntity columns to user_coupon_usage safely

ALTER TABLE user_coupon_usage ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE user_coupon_usage ADD COLUMN IF NOT EXISTS modified_by VARCHAR(255);
ALTER TABLE user_coupon_usage ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
