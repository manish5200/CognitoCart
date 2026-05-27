-- V24: Product return policy table
-- Stores return/exchange/replacement rules at product OR category level.
-- BaseEntity columns (id, created_at, updated_at, created_by, modified_by, version) MUST be present.

CREATE TABLE product_return_policy (
    -- BaseEntity fields
    id                   BIGSERIAL       PRIMARY KEY,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP,
    created_by           VARCHAR(255),
    modified_by          VARCHAR(255),
    version              BIGINT          NOT NULL DEFAULT 0,   -- Hibernate optimistic lock

    -- Business fields
    product_id           BIGINT          REFERENCES products(id)    ON DELETE CASCADE,
    category_id          BIGINT          REFERENCES categories(id)  ON DELETE CASCADE,
    policy_type          VARCHAR(30)     NOT NULL,
    return_window_days   INT             NOT NULL DEFAULT 7,
    return_allowed       BOOLEAN         NOT NULL DEFAULT TRUE,
    exchange_allowed     BOOLEAN         NOT NULL DEFAULT FALSE,
    replacement_allowed  BOOLEAN         NOT NULL DEFAULT FALSE,
    pickup_available     BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Exactly one of product_id or category_id must be set (never both, never neither)
    CONSTRAINT chk_policy_target CHECK (
        (product_id IS NOT NULL AND category_id IS NULL) OR
        (product_id IS NULL     AND category_id IS NOT NULL)
    )
);

CREATE INDEX idx_policy_product  ON product_return_policy(product_id);
CREATE INDEX idx_policy_category ON product_return_policy(category_id);
