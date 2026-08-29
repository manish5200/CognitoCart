-- WHY THIS MIGRATION EXISTS:
-- The old IVFFlat index (lists = 100) is like a library card catalog that only
-- works well when there are thousands of books. With fewer than ~300 products,
-- it returns random/wrong results because it can't find its cluster centers.
--
-- HNSW (Hierarchical Navigable Small World) is a different algorithm that works
-- correctly from 1 product up to millions. It also has better accuracy
-- (higher recall) even on large datasets. It is the industry standard today.

-- Step 1: Drop the old broken index
DROP INDEX IF EXISTS idx_product_embedding;

-- Step 2: Create the HNSW index on the 384-dim embedding column
-- m = 16           -> each node links to 16 neighbours (speed vs accuracy tradeoff)
-- ef_construction  -> how carefully the graph is built (64 = good default)
CREATE INDEX IF NOT EXISTS idx_product_embedding
    ON products USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
