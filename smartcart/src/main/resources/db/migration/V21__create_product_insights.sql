CREATE TABLE product_insights (
    --ProductInsights- specific table
    product_id BIGINT NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE ,
    ai_summary TEXT,
    total_reviews BIGINT DEFAULT 0,
    last_generated TIMESTAMP,

    -- BaseEntity fields (inherited by extends BaseEntity)
    id              BIGSERIAL       PRIMARY KEY,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    modified_by     VARCHAR(255),
    version         BIGINT          DEFAULT 0
);