-- CONCEPT: We are switching from OpenAI's text-embedding-3-small (1536 dimensions)
-- to HuggingFace's sentence-transformers/all-MiniLM-L6-v2 (384 dimensions).
--
-- WHY RESIZE? The stored vectors and the query vector MUST be the same size.
-- If we stored 1536-dim vectors but query with 384-dim, PostgreSQL throws an error.
-- We drop and recreate the column + index to match the new model's output size.
--
-- NOTE: Any existing embeddings are lost, but that's fine — they were NULL anyway
-- (OpenAI had no credits, so no embeddings were ever successfully stored).

-- Step 1: Drop the old 1536-dim column and its index
DROP INDEX IF EXISTS idx_product_embedding;
ALTER TABLE products DROP COLUMN IF EXISTS embedding;

-- Step 2: Add the new 384-dim column for the HuggingFace model
-- all-MiniLM-L6-v2 produces exactly 384 floating-point numbers per text input
ALTER TABLE products ADD COLUMN embedding vector(384);

-- Step 3: Recreate the IVFFlat cosine similarity index for the new dimension
-- lists = 100: divides vectors into 100 buckets for fast Approximate Nearest Neighbor search
CREATE INDEX IF NOT EXISTS idx_product_embedding
    ON products USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
