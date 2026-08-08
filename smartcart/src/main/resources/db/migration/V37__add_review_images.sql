-- Stores Cloudinary CDN URLs for customer-uploaded review photos.
-- Separate table (not JSONB) to allow future indexing and moderation queries.
CREATE TABLE review_images (
                               review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
                               image_url TEXT NOT NULL
);
CREATE INDEX idx_review_images_review_id ON review_images(review_id);
