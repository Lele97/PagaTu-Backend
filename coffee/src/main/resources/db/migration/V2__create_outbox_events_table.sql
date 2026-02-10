-- Outbox Events Table for Transactional Outbox Pattern
-- This table stores events to be published to NATS

CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGSERIAL PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(500)
);

-- Index for efficient polling of unprocessed events
CREATE INDEX IF NOT EXISTS idx_outbox_unprocessed 
ON outbox_events (processed_at, created_at) 
WHERE processed_at IS NULL;

-- Index for cleanup job
CREATE INDEX IF NOT EXISTS idx_outbox_processed_at 
ON outbox_events (processed_at) 
WHERE processed_at IS NOT NULL;

-- Comment on table
COMMENT ON TABLE outbox_events IS 'Transactional outbox for reliable event delivery to NATS';
COMMENT ON COLUMN outbox_events.subject IS 'NATS subject/topic to publish to';
COMMENT ON COLUMN outbox_events.event_type IS 'Event class name for debugging';
COMMENT ON COLUMN outbox_events.payload IS 'Serialized JSON event payload';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of failed publish attempts';
COMMENT ON COLUMN outbox_events.last_error IS 'Last error message if publishing failed';
