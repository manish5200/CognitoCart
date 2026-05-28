-- V26: Add return_proof_images column to orders table
-- Stores a JSON array of Cloudinary CDN URLs uploaded as proof during a return request.
-- e.g. ["https://res.cloudinary.com/.../returns/42/img1.jpg", "..."]
-- Using JSONB (binary JSON) for efficient storage and querying.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_proof_images JSONB;

-- Default NULL means: no images uploaded (standard/change-of-mind returns)
-- Non-NULL means: DEFECTIVE/WRONG_ITEM claim with photographic proof
