-- safely track when we last emailed a user about a specific wishlist product hitting a sale price
-- prevents spamming the user every single day if the sale lasts a month
ALTER TABLE user_wishlist ADD COLUMN IF NOT EXISTS last_price_drop_notified_at TIMESTAMP;

-- add discount price to track actual sale values
ALTER TABLE products ADD COLUMN IF NOT EXISTS discount_price NUMERIC(38,2);
