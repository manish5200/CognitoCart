-- 1. Create Refresh Token Table
CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                token VARCHAR(255) UNIQUE NOT NULL,
                                expiry_date TIMESTAMP NOT NULL,
                                user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                revoked BOOLEAN DEFAULT FALSE,
                                created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP WITHOUT TIME ZONE,
                                created_by VARCHAR(255),
                                modified_by VARCHAR(255),
                                version BIGINT DEFAULT 0
);

-- 2. Add Audit Fields to Profiles
ALTER TABLE customer_profiles
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN created_by VARCHAR(255),
    ADD COLUMN modified_by VARCHAR(255),
    ADD COLUMN version BIGINT DEFAULT 0;

ALTER TABLE seller_profiles
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN created_by VARCHAR(255),
    ADD COLUMN modified_by VARCHAR(255),
    ADD COLUMN version BIGINT DEFAULT 0;

-- 3. Add Audit Fields to Order Items (The missing piece we found!)
ALTER TABLE order_items
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN created_by VARCHAR(255),
    ADD COLUMN modified_by VARCHAR(255),
    ADD COLUMN version BIGINT DEFAULT 0;