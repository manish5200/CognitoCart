-- The vector extension was pre-installed manually as a superuser (one-time system setup).
-- Flyway cannot run CREATE EXTENSION because it requires superuser privileges.

-- Add the embedding column to store OpenAI's 1536-dimensional meaning vector per product
ALTER TABLE products ADD COLUMN IF NOT EXISTS embedding vector(1536);

-- IVFFlat index for fast Approximate Nearest Neighbor cosine similarity search
CREATE INDEX IF NOT EXISTS idx_product_embedding
    ON products USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
