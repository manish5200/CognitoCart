-- Creates the master table for Admin-controlled marketing events
CREATE TABLE platform_sale_events (
                                      id BIGSERIAL PRIMARY KEY,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP,
                                      created_by VARCHAR(255),
                                      modified_by VARCHAR(255),
                                      version BIGINT DEFAULT 0, -- Inherited from BaseEntity for Optimistic Locking

                                      event_name VARCHAR(150) NOT NULL UNIQUE,
                                      description VARCHAR(500),
                                      start_time TIMESTAMP NOT NULL,
                                      end_time TIMESTAMP NOT NULL,
                                      status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
);

-- Composite index optimizes the ShedLock background job that runs every minute
CREATE INDEX idx_event_status_times ON platform_sale_events (status, start_time, end_time);

-- Creates the child table for Seller-submitted discounted SKUs
CREATE TABLE flash_sale_items (
                                  id BIGSERIAL PRIMARY KEY,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP,
                                  created_by VARCHAR(255),
                                  modified_by VARCHAR(255),
                                  version BIGINT DEFAULT 0, -- Inherited from BaseEntity for Optimistic Locking

                                  platform_sale_event_id BIGINT NOT NULL,
                                  product_variant_id BIGINT NOT NULL,
                                  seller_id BIGINT NOT NULL,
                                  discount_percentage NUMERIC(5,2) NOT NULL,
                                  max_units INT NOT NULL,
                                  max_units_per_user INT NOT NULL DEFAULT 1,
                                  used_units INT NOT NULL DEFAULT 0,
                                  approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

                                  CONSTRAINT fk_fsi_event FOREIGN KEY (platform_sale_event_id) REFERENCES platform_sale_events(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_fsi_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id) ON DELETE CASCADE
);

-- Indexes optimized for CartService joins and Redis cache loads
CREATE INDEX idx_fsi_approval ON flash_sale_items (approval_status);
CREATE INDEX idx_fsi_event ON flash_sale_items (platform_sale_event_id);
CREATE INDEX idx_fsi_variant ON flash_sale_items (product_variant_id);
