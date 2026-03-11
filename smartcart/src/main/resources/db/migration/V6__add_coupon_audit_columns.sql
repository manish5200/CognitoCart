-- V6__add_coupon_audit_columns.sql
-- The original V5 created the coupons table without BaseEntity audit columns.
-- This migration adds the missing columns that Hibernate expects from BaseEntity.
ALTER TABLE coupons
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS modified_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
