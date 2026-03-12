-- V10: Add payment_status to orders table
-- Tracks the payment lifecycle independently of the order fulfillment lifecycle.
-- This allows clean separation between:
--   "What state is the order in?" (orderStatus: CONFIRMED, SHIPPED, DELIVERED)
--   "What state is the payment in?" (paymentStatus: PENDING, PAID, FAILED, REFUNDED)

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(30) DEFAULT 'PENDING';

-- Backfill existing rows: any order already marked PAID in order_status gets PAID here too
UPDATE orders SET payment_status = 'PAID'   WHERE order_status = 'PAID';
UPDATE orders SET payment_status = 'PENDING' WHERE order_status = 'PAYMENT_PENDING';
UPDATE orders SET payment_status = 'PENDING' WHERE payment_status IS NULL;
