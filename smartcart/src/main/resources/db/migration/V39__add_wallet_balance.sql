-- 1. Add wallet to customer profile
ALTER TABLE customer_profiles ADD COLUMN wallet_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

-- 2. Add split-tender tracking to orders so invoices and refunds stay mathematically accurate
ALTER TABLE orders ADD COLUMN wallet_amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE orders ADD COLUMN gateway_amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0.00;
