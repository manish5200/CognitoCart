-- V7: Add Razorpay integration fields to the orders table

ALTER TABLE orders
ADD COLUMN razorpay_order_id VARCHAR(255),
ADD COLUMN razorpay_payment_id VARCHAR(255),
ADD COLUMN razorpay_signature VARCHAR(255);

-- Create an index on razorpay_order_id since we will query by it to verify payments
CREATE INDEX idx_orders_razorpay_order_id ON orders(razorpay_order_id);
