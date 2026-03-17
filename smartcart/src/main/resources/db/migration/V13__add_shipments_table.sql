-- Creates the shipments table to store courier and tracking details per order.
-- UNIQUE constraint on order_id enforces the One-to-One relationship at DB level.
-- BaseEntity provides: id, created_at, updated_at, created_by, modified_by, version

CREATE TABLE shipments (
                           id                      BIGSERIAL       PRIMARY KEY,
                           created_at              TIMESTAMP       NOT NULL,
                           updated_at              TIMESTAMP,
                           created_by              VARCHAR(255),
                           modified_by             VARCHAR(255),
                           version                 BIGINT DEFAULT 0,

    -- The order this shipment belongs to (One-to-One)
                           order_id                BIGINT          NOT NULL UNIQUE,

    -- Courier details
                           courier_name            VARCHAR(100),
                           tracking_number         VARCHAR(100),
                           tracking_url            VARCHAR(500),
                           estimated_delivery_date DATE,
                           dispatched_by           VARCHAR(150),

                           CONSTRAINT fk_shipment_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
