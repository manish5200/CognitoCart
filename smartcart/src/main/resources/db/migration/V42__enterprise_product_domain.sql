-- 1. Add Core Domain Attributes to Products
ALTER TABLE products
    ADD COLUMN country_of_origin VARCHAR(2), -- ISO 3166-1 alpha-2
    ADD COLUMN condition VARCHAR(30),        -- NEW, USED, REFURBISHED
    ADD COLUMN product_type VARCHAR(30),     -- PHYSICAL, DIGITAL
    ADD COLUMN lifecycle_status VARCHAR(30) DEFAULT 'DRAFT' NOT NULL,
    ADD COLUMN approval_status VARCHAR(30) DEFAULT 'NOT_REQUIRED' NOT NULL,
    -- ProductWarranty Embeddable
    ADD COLUMN warranty_type VARCHAR(30) DEFAULT 'NONE' NOT NULL,
    ADD COLUMN warranty_duration INTEGER,
    ADD COLUMN warranty_duration_unit VARCHAR(20),
    -- ProductSEO Embeddable
    ADD COLUMN seo_title VARCHAR(255),
    ADD COLUMN meta_description VARCHAR(500);

-- Enforce slug uniqueness for collision retries
ALTER TABLE products ADD CONSTRAINT uk_product_slug UNIQUE (slug);

-- 2. Create Product Moderation History
CREATE TABLE product_moderation_history (
                                            id BIGINT PRIMARY KEY,
                                            product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                            admin_id BIGINT, -- Nullable for SYSTEM
                                            actor_type VARCHAR(20) NOT NULL, -- ADMIN, SYSTEM
                                            action VARCHAR(50) NOT NULL, -- SUBMITTED, APPROVED, REJECTED, REQUESTED_CHANGES
                                            approval_status_from VARCHAR(30),
                                            approval_status_to VARCHAR(30) NOT NULL,
                                            reason TEXT,
                                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE IF NOT EXISTS product_moderation_history_seq START WITH 100000 INCREMENT BY 50;

-- 3. Create Category Attribute Definitions
CREATE TABLE category_attribute_definitions (
                                                id BIGINT PRIMARY KEY,
                                                category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
                                                attribute_key VARCHAR(100) NOT NULL,
                                                display_name VARCHAR(150) NOT NULL,
                                                data_type VARCHAR(30) NOT NULL, -- TEXT, NUMBER, SELECT, BOOLEAN
                                                scope VARCHAR(20) NOT NULL, -- PRODUCT, VARIANT
                                                is_required BOOLEAN DEFAULT false NOT NULL,
                                                is_filterable BOOLEAN DEFAULT false NOT NULL,
                                                is_searchable BOOLEAN DEFAULT false NOT NULL,
                                                sort_order INTEGER DEFAULT 0 NOT NULL,
                                                options JSONB, -- For SELECT types
                                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                updated_at TIMESTAMP,
                                                created_by VARCHAR(255),
                                                modified_by VARCHAR(255),
                                                version BIGINT DEFAULT 0,
                                                CONSTRAINT uk_category_attribute UNIQUE (category_id, attribute_key)
);

CREATE SEQUENCE IF NOT EXISTS category_attribute_def_seq START WITH 100000 INCREMENT BY 50;

-- 4. Add product-level JSONB attributes storage
ALTER TABLE products ADD COLUMN attributes JSONB;
