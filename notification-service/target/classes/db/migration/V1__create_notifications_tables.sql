-- Tabla para la idempotencia (la que creamos en la Issue 21)
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
);

-- Tabla para la auditoría (Issue 22)
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    original_event_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    recipient VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    attempt_count INT DEFAULT 1,
    dispatched_at TIMESTAMP NOT NULL
);