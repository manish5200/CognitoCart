CREATE TABLE failed_webhook_events (
                                       id BIGSERIAL PRIMARY KEY,

    -- DLQ specific columns
                                       payload TEXT NOT NULL,
                                       signature VARCHAR(255) NOT NULL,
                                       event_type VARCHAR(255),
                                       error_message TEXT,
                                       status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    -- BaseEntity columns
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP,
                                       created_by VARCHAR(255),
                                       modified_by VARCHAR(255),
                                       version BIGINT NOT NULL DEFAULT 0
);

-- Index to quickly find PENDING webhooks that need to be replayed
CREATE INDEX idx_failed_webhooks_status ON failed_webhook_events(status);
