-- V25: Add return request fields to the orders table.
-- return_policy_snapshot (JSONB) freezes the policy active at checkout time,
-- so future policy changes never retroactively affect existing orders.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_reason           VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_description      VARCHAR(500);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_requested_at     TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivered_at            TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_policy_snapshot  JSONB;
-- V26: Track what the customer REQUESTED: RETURN, REPLACEMENT, or EXCHANGE.
-- Separate from return_reason (WHY: DEFECTIVE, WRONG_ITEM)
-- and return_policy_snapshot (the seller's frozen rules at checkout time).
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_request_type VARCHAR(20);

