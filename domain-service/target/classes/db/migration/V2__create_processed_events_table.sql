CREATE TABLE IF NOT EXISTS domain.processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);

-- Opcional: Índice para mejorar la velocidad de búsqueda de idempotencia
CREATE INDEX IF NOT EXISTS idx_processed_events_type ON domain.processed_events(event_type);