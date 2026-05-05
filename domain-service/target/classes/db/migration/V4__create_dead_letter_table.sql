-- =============================================
-- SCHEMA: domain - Dead Letter Events
-- =============================================

CREATE TABLE domain.dead_letter_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_topic VARCHAR(255) NOT NULL,
    event_id       UUID,
    event_type     VARCHAR(255),
    payload        TEXT NOT NULL,
    error_message  TEXT,
    failed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    retry_count    INT DEFAULT 0
);

-- Índice para búsquedas rápidas por ID de evento original
CREATE INDEX ON domain.dead_letter_events(event_id);