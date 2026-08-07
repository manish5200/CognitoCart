-- ─────────────────────────────────────────────────────────────────────────────
-- ORDER EVENTS: Immutable audit trail of every order status transition.
-- Mirrors the BaseEntity schema (id, public_id, auditing, version) used by
-- all other domain tables in this application for structural consistency.
-- Every setOrderStatus() call in the application MUST produce a row here.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE SEQUENCE IF NOT EXISTS order_event_seq START 1 INCREMENT 50;

CREATE TABLE order_events (
    -- BaseEntity columns (consistent with orders, products, variants, etc.)
                              id          BIGINT       NOT NULL DEFAULT nextval('order_event_seq'),
                              public_id   UUID         NOT NULL DEFAULT gen_random_uuid(),
                              created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),  -- This IS the event timestamp
                              updated_at  TIMESTAMP,
                              created_by  VARCHAR(255),
                              modified_by VARCHAR(255),
                              version     BIGINT       NOT NULL DEFAULT 0,

    -- Event-specific columns
                              order_id    BIGINT       NOT NULL,
                              status      VARCHAR(40)  NOT NULL,  -- OrderStatus enum value
                              actor       VARCHAR(100) NOT NULL,  -- "SYSTEM" | "SELLER:42" | "CUSTOMER:7" | "ADMIN:1"
                              note        TEXT,                   -- Optional: "Razorpay: pay_xxx", "AWB: BDL123456"

                              CONSTRAINT pk_order_events        PRIMARY KEY (id),
                              CONSTRAINT uq_order_events_pubid  UNIQUE (public_id),
                              CONSTRAINT fk_order_events_order  FOREIGN KEY (order_id)
                                  REFERENCES orders(id) ON DELETE CASCADE
);

-- Fast chronological timeline lookup per order
CREATE INDEX idx_order_events_order_id ON order_events(order_id);
