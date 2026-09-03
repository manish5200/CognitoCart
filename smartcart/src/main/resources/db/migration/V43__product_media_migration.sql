-- 1. Create ProductMedia Table
CREATE TABLE product_media (
                               id BIGINT PRIMARY KEY,
                               product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                               media_url VARCHAR(1000) NOT NULL,
                               public_id VARCHAR(255),
                               media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
                               is_primary BOOLEAN NOT NULL DEFAULT false,
                               sort_order INTEGER NOT NULL DEFAULT 0,
                               alt_text VARCHAR(500),
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP,
                               created_by VARCHAR(255),
                               modified_by VARCHAR(255),
                               version BIGINT DEFAULT 0
);

CREATE SEQUENCE IF NOT EXISTS product_media_seq START WITH 100000 INCREMENT BY 50;

-- 2. Constraints for strict data integrity
CREATE UNIQUE INDEX idx_primary_media ON product_media(product_id) WHERE is_primary = true;
ALTER TABLE product_media ADD CONSTRAINT uk_product_media_sort UNIQUE (product_id, sort_order);

-- 3. Deterministic Data Migration
-- We filter out dirty data (nulls/empties), then assign exactly one primary image (row=1)
-- and a zero-based sort_order (row-1) alphabetically by URL.
INSERT INTO product_media (id, product_id, media_url, media_type, sort_order, is_primary, created_at)
SELECT
    nextval('product_media_seq'),
    product_id,
    image_url,
    'IMAGE',
    (ROW_NUMBER() OVER(PARTITION BY product_id ORDER BY image_url ASC)) - 1,
    (ROW_NUMBER() OVER(PARTITION BY product_id ORDER BY image_url ASC) = 1),
    CURRENT_TIMESTAMP
FROM product_images
WHERE image_url IS NOT NULL AND TRIM(image_url) <> '';

-- NOTE: We retain product_images table for rollback safety.
-- It will be dropped in a future major version release.
